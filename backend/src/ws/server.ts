import { WebSocketServer, WebSocket } from 'ws';
import type { Server } from 'node:http';
import { verifyJwt } from '../auth/jwt.js';
import { db } from '../db/client.js';

interface ClientMeta {
  userId: string;
  circleId: string;
  cosmicName: string;
}

const circleClients = new Map<string, Set<WebSocket>>();
const clientMeta = new WeakMap<WebSocket, ClientMeta>();

export const circleBroadcasters = new Map<string, (msg: Record<string, unknown>) => void>();

function broadcast(circleId: string, msg: Record<string, unknown>) {
  const set = circleClients.get(circleId);
  if (!set) return;
  const data = JSON.stringify(msg);
  for (const ws of set) if (ws.readyState === WebSocket.OPEN) ws.send(data);
}

/**
 * Who is actually focusing right now, read from the sessions table rather than
 * from who happens to hold a socket open. A session counts as live while it is
 * neither completed nor broken and its planned window has not yet elapsed, so a
 * session the user abandoned without telling us drops out on its own.
 */
export async function focusingIn(circleId: string) {
  const now = Date.now();
  const rows = await db.all<any>(
    `SELECT u.id, u.cosmic_name, u.avatar_seed, s.started_at, s.planned_min, s.intention
     FROM circle_members m
     JOIN users u   ON u.id = m.user_id
     JOIN sessions s ON s.user_id = u.id
     WHERE m.circle_id = ?
       AND s.completed = 0
       AND s.broken_at IS NULL
       AND s.started_at + s.planned_min * 60000 > ?
     ORDER BY s.started_at ASC`,
    [circleId, now]
  );
  return rows.map(r => ({
    userId: r.id,
    cosmicName: r.cosmic_name,
    avatarSeed: r.avatar_seed,
    startedAt: Number(r.started_at),
    plannedMin: Number(r.planned_min),
    endsAt: Number(r.started_at) + Number(r.planned_min) * 60000,
    intention: r.intention ?? null,
  }));
}

async function presenceSnapshot(circleId: string) {
  const set = circleClients.get(circleId);
  const online = set
    ? Array.from(new Set(Array.from(set).map(ws => clientMeta.get(ws)?.cosmicName).filter(Boolean)))
    : [];
  return { type: 'presence', circleId, online, focusing: await focusingIn(circleId) };
}

/** Push a fresh presence snapshot to everyone watching this circle. */
export async function broadcastPresence(circleId: string) {
  if (!circleClients.has(circleId)) return;
  broadcast(circleId, await presenceSnapshot(circleId));
}

export function attachWs(httpServer: Server) {
  const wss = new WebSocketServer({ noServer: true });

  httpServer.on('upgrade', (req, socket, head) => {
    const url = new URL(req.url ?? '', `http://${req.headers.host}`);
    const match = url.pathname.match(/^\/ws\/circles\/([a-zA-Z0-9-]+)$/);
    if (!match) { socket.destroy(); return; }

    const circleId = match[1];
    const token = url.searchParams.get('token') ?? '';

    let userId: string;
    try { userId = verifyJwt(token).uid; } catch { socket.destroy(); return; }

    // DB lookups are async now, so resolve membership before upgrading.
    void (async () => {
      const member = await db.get('SELECT 1 FROM circle_members WHERE circle_id = ? AND user_id = ?', [
        circleId, userId,
      ]);
      if (!member) { socket.destroy(); return; }

      const u = await db.get<any>('SELECT cosmic_name FROM users WHERE id = ?', [userId]);
      const name = u?.cosmic_name ?? 'Unknown';

      wss.handleUpgrade(req, socket, head, ws => {
        clientMeta.set(ws, { userId, circleId, cosmicName: name });
        if (!circleClients.has(circleId)) circleClients.set(circleId, new Set());
        circleClients.get(circleId)!.add(ws);
        circleBroadcasters.set(circleId, msg => broadcast(circleId, msg));
        void broadcastPresence(circleId);

        ws.on('message', raw => {
          try {
            const msg = JSON.parse(String(raw));
            // Clients ping to keep the socket alive through mobile NAT timeouts.
            if (msg?.type === 'ping') ws.send(JSON.stringify({ type: 'pong' }));
            // A client may ask for a fresh snapshot after reconnecting.
            if (msg?.type === 'presence?') void broadcastPresence(circleId);
          } catch { /* ignore malformed frames */ }
        });

        ws.on('close', () => {
          const set = circleClients.get(circleId);
          set?.delete(ws);
          if (set && set.size === 0) {
            circleClients.delete(circleId);
            circleBroadcasters.delete(circleId);
            return; // nobody left to notify
          }
          void broadcastPresence(circleId);
        });
      });
    })();
  });
}
