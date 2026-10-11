import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';
import { uuid } from '../lib/uuid.js';

export const subjectsRouter = Router();
subjectsRouter.use(requireAuth);

subjectsRouter.get('/', (req, res) => {
  const { userId } = req as AuthedRequest;
  const rows = db
    .prepare('SELECT id, name, color, total_min, planet_state FROM subjects WHERE user_id = ? ORDER BY created_at')
    .all(userId) as any[];
  res.json(rows.map(r => ({ id: r.id, name: r.name, color: r.color, totalMin: r.total_min, planetState: JSON.parse(r.planet_state) })));
});

const createBody = z.object({ name: z.string().min(1).max(32), color: z.string().regex(/^#[0-9A-Fa-f]{6}$/) });

subjectsRouter.post('/', (req, res) => {
  const parsed = createBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const id = uuid();
  db.prepare('INSERT INTO subjects (id, user_id, name, color, created_at) VALUES (?, ?, ?, ?, ?)')
    .run(id, userId, parsed.data.name, parsed.data.color, Date.now());
  res.json({ id, name: parsed.data.name, color: parsed.data.color, totalMin: 0, planetState: {} });
});
