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

function presenceSnapshot(circleId: string) {
  const set = circleClients.get(circleId);
  const focusing = set
    ? Array.from(set).map(ws => clientMeta.get(ws)?.cosmicName).filter(Boolean)
    : [];
  return { type: 'presence', circleId, focusing };
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
        broadcast(circleId, presenceSnapshot(circleId));

        ws.on('message', raw => {
          try {
            const msg = JSON.parse(String(raw));
            if (msg?.type === 'ping') ws.send(JSON.stringify({ type: 'pong' }));
          } catch { /* ignore malformed frames */ }
        });

        ws.on('close', () => {
          const set = circleClients.get(circleId);
          set?.delete(ws);
          if (set && set.size === 0) {
            circleClients.delete(circleId);
            circleBroadcasters.delete(circleId);
          }
          broadcast(circleId, presenceSnapshot(circleId));
        });
      });
    })();
  });
}
