import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';
import { uuid } from '../lib/uuid.js';

export const subjectsRouter = Router();
subjectsRouter.use(requireAuth);

subjectsRouter.get('/', async (req, res) => {
  const { userId } = req as AuthedRequest;
  const rows = await db.all<any>(
    'SELECT id, name, color, total_min, planet_state FROM subjects WHERE user_id = ? ORDER BY created_at',
    [userId]
  );
  res.json(rows.map(r => ({
    id: r.id,
    name: r.name,
    color: r.color,
    totalMin: Number(r.total_min),
    planetState: JSON.parse(r.planet_state),
  })));
});

const createBody = z.object({
  name: z.string().min(1).max(32),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/),
});

subjectsRouter.post('/', async (req, res) => {
  const parsed = createBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;

  // Don't create duplicate planets for the same name.
  const existing = await db.get<any>(
    'SELECT id, name, color, total_min FROM subjects WHERE user_id = ? AND name = ? COLLATE NOCASE',
    [userId, parsed.data.name]
  );
  if (existing) {
    return res.json({
      id: existing.id, name: existing.name, color: existing.color,
      totalMin: Number(existing.total_min), planetState: {},
    });
  }

  const id = uuid();
  await db.run(
    'INSERT INTO subjects (id, user_id, name, color, created_at) VALUES (?, ?, ?, ?, ?)',
    [id, userId, parsed.data.name, parsed.data.color, Date.now()]
  );
  res.json({ id, name: parsed.data.name, color: parsed.data.color, totalMin: 0, planetState: {} });
});
