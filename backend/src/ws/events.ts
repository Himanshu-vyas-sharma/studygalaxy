import { db } from '../db/client.js';
import { uuid } from '../lib/uuid.js';
import { circleBroadcasters, broadcastPresence } from './server.js';

export async function broadcastCircleEvent(userId: string, payload: Record<string, unknown>) {
  const circles = await db.all<any>('SELECT circle_id FROM circle_members WHERE user_id = ?', [userId]);
  if (circles.length === 0) return;

  const u = await db.get<any>('SELECT cosmic_name FROM users WHERE id = ?', [userId]);
  const cosmicName = u?.cosmic_name ?? 'Unknown';
  const at = Date.now();
  const type = String(payload.type ?? 'event');
  const json = JSON.stringify(payload);

  await db.batch(
    circles.map(row => ({
      sql: 'INSERT INTO circle_events (id, circle_id, user_id, type, payload, created_at) VALUES (?, ?, ?, ?, ?, ?)',
      args: [uuid(), row.circle_id, userId, type, json, at],
    }))
  );

  for (const row of circles) {
    circleBroadcasters.get(row.circle_id)?.({ ...payload, userId, cosmicName, at });
    // Starting, finishing or breaking a session all change who is focusing,
    // so follow every event with a fresh presence snapshot.
    await broadcastPresence(row.circle_id);
  }
}
