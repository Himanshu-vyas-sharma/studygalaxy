import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';
import { uuid, inviteCode } from '../lib/uuid.js';

export const circlesRouter = Router();
circlesRouter.use(requireAuth);

const createBody = z.object({ name: z.string().min(1).max(40) });

circlesRouter.post('/', (req, res) => {
  const parsed = createBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const id = uuid();
  let code = inviteCode();
  // handle (very rare) collision
  for (let i = 0; i < 5; i++) {
    const exists = db.prepare('SELECT 1 FROM circles WHERE invite_code = ?').get(code);
    if (!exists) break;
    code = inviteCode();
  }
  const now = Date.now();
  db.prepare('INSERT INTO circles (id, name, invite_code, created_by, created_at) VALUES (?, ?, ?, ?, ?)')
    .run(id, parsed.data.name, code, userId, now);
  db.prepare('INSERT INTO circle_members (circle_id, user_id, joined_at) VALUES (?, ?, ?)').run(id, userId, now);
  res.json({ id, name: parsed.data.name, inviteCode: code });
});

const joinBody = z.object({ inviteCode: z.string().min(4).max(10) });

circlesRouter.post('/join', (req, res) => {
  const parsed = joinBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const circle = db.prepare('SELECT id, name FROM circles WHERE invite_code = ?').get(parsed.data.inviteCode.toUpperCase()) as any;
  if (!circle) return res.status(404).json({ error: 'bad_code' });
  db.prepare('INSERT OR IGNORE INTO circle_members (circle_id, user_id, joined_at) VALUES (?, ?, ?)')
    .run(circle.id, userId, Date.now());
  res.json({ id: circle.id, name: circle.name });
});

circlesRouter.get('/', (req, res) => {
  const { userId } = req as AuthedRequest;
  const rows = db
    .prepare(`SELECT c.id, c.name, c.invite_code FROM circles c
              JOIN circle_members m ON m.circle_id = c.id
              WHERE m.user_id = ? ORDER BY m.joined_at DESC`)
    .all(userId) as any[];
  res.json(rows.map(r => ({ id: r.id, name: r.name, inviteCode: r.invite_code })));
});

circlesRouter.get('/:id/events', (req, res) => {
  const { userId } = req as AuthedRequest;
  const member = db.prepare('SELECT 1 FROM circle_members WHERE circle_id = ? AND user_id = ?').get(req.params.id, userId);
  if (!member) return res.status(403).json({ error: 'not_member' });
  const rows = db
    .prepare(`SELECT e.id, e.type, e.payload, e.created_at, u.cosmic_name
              FROM circle_events e JOIN users u ON u.id = e.user_id
              WHERE e.circle_id = ? ORDER BY e.created_at DESC LIMIT 50`)
    .all(req.params.id) as any[];
  res.json(rows.map(r => ({ id: r.id, type: r.type, payload: JSON.parse(r.payload), at: r.created_at, cosmicName: r.cosmic_name })));
});
