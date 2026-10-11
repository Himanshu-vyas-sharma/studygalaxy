import { db } from '../db/client.js';
import { uuid } from '../lib/uuid.js';
import { circleBroadcasters } from './server.js';

export function broadcastCircleEvent(userId: string, payload: Record<string, unknown>) {
  const circles = db
    .prepare('SELECT circle_id FROM circle_members WHERE user_id = ?')
    .all(userId) as any[];
  const cosmicName = (db.prepare('SELECT cosmic_name FROM users WHERE id = ?').get(userId) as any)?.cosmic_name ?? 'Unknown';
  const at = Date.now();

  for (const row of circles) {
    const circleId = row.circle_id;
    db.prepare('INSERT INTO circle_events (id, circle_id, user_id, type, payload, created_at) VALUES (?, ?, ?, ?, ?, ?)')
      .run(uuid(), circleId, userId, String(payload.type ?? 'event'), JSON.stringify(payload), at);
    const bc = circleBroadcasters.get(circleId);
    bc?.({ ...payload, userId, cosmicName, at });
  }
}
