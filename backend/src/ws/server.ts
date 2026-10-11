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
  const names = set ? Array.from(set).map(ws => clientMeta.get(ws)?.cosmicName).filter(Boolean) : [];
  return { type: 'presence', circleId, focusing: names };
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
    const member = db.prepare('SELECT 1 FROM circle_members WHERE circle_id = ? AND user_id = ?').get(circleId, userId);
    if (!member) { socket.destroy(); return; }
    const name = (db.prepare('SELECT cosmic_name FROM users WHERE id = ?').get(userId) as any)?.cosmic_name ?? 'Unknown';

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
        } catch { /* ignore */ }
      });

      ws.on('close', () => {
        circleClients.get(circleId)?.delete(ws);
        broadcast(circleId, presenceSnapshot(circleId));
      });
    });
  });
}
