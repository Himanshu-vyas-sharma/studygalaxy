import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';
import { uuid, inviteCode } from '../lib/uuid.js';

export const circlesRouter = Router();
circlesRouter.use(requireAuth);

const authed = (req: unknown) => req as AuthedRequest;

const createBody = z.object({ name: z.string().min(1).max(40) });

circlesRouter.post('/', async (req, res) => {
  const parsed = createBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;

  let code = inviteCode();
  for (let i = 0; i < 5; i++) {
    const exists = await db.get('SELECT 1 FROM circles WHERE invite_code = ?', [code]);
    if (!exists) break;
    code = inviteCode();
  }

  const id = uuid();
  const now = Date.now();
  await db.batch([
    { sql: 'INSERT INTO circles (id, name, invite_code, created_by, created_at) VALUES (?, ?, ?, ?, ?)',
      args: [id, parsed.data.name, code, userId, now] },
    { sql: 'INSERT INTO circle_members (circle_id, user_id, joined_at) VALUES (?, ?, ?)',
      args: [id, userId, now] },
  ]);
  res.json({ id, name: parsed.data.name, inviteCode: code });
});

const joinBody = z.object({ inviteCode: z.string().min(4).max(10) });

circlesRouter.post('/join', async (req, res) => {
  const parsed = joinBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;

  const circle = await db.get<any>('SELECT id, name FROM circles WHERE invite_code = ?', [
    parsed.data.inviteCode.toUpperCase(),
  ]);
  if (!circle) return res.status(404).json({ error: 'bad_code' });

  await db.run('INSERT OR IGNORE INTO circle_members (circle_id, user_id, joined_at) VALUES (?, ?, ?)', [
    circle.id, userId, Date.now(),
  ]);
  res.json({ id: circle.id, name: circle.name });
});

circlesRouter.get('/', async (req, res) => {
  const { userId } = req as AuthedRequest;
  const rows = await db.all<any>(
    `SELECT c.id, c.name, c.invite_code FROM circles c
     JOIN circle_members m ON m.circle_id = c.id
     WHERE m.user_id = ? ORDER BY m.joined_at DESC`,
    [userId]
  );
  res.json(rows.map(r => ({ id: r.id, name: r.name, inviteCode: r.invite_code })));
});

circlesRouter.get('/:id/events', async (req, res) => {
  const { userId } = authed(req);
  const member = await db.get('SELECT 1 FROM circle_members WHERE circle_id = ? AND user_id = ?', [
    req.params.id, userId,
  ]);
  if (!member) return res.status(403).json({ error: 'not_member' });

  const rows = await db.all<any>(
    `SELECT e.id, e.type, e.payload, e.created_at, u.cosmic_name
     FROM circle_events e JOIN users u ON u.id = e.user_id
     WHERE e.circle_id = ? ORDER BY e.created_at DESC LIMIT 50`,
    [req.params.id]
  );
  res.json(rows.map(r => ({
    id: r.id,
    type: r.type,
    payload: JSON.parse(r.payload),
    at: Number(r.created_at),
    cosmicName: r.cosmic_name,
  })));
});
