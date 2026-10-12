import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';
import { uuid } from '../lib/uuid.js';

export const notesRouter = Router();
notesRouter.use(requireAuth);

const strokeSchema = z.object({
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/),
  width: z.number().min(0.5).max(40),
  erase: z.boolean().optional(),
  points: z.array(z.tuple([z.number(), z.number()])).min(1).max(5000),
});
const strokesSchema = z.array(strokeSchema).max(500);

const authed = (req: unknown) => req as AuthedRequest;

notesRouter.get('/', async (req, res) => {
  const { userId } = authed(req);
  const rows = await db.all<any>(
    'SELECT id, subject_id, title, strokes_json, created_at, updated_at FROM notes WHERE user_id = ? ORDER BY updated_at DESC',
    [userId]
  );
  res.json(rows.map(r => ({
    id: r.id,
    subjectId: r.subject_id,
    title: r.title,
    strokes: JSON.parse(r.strokes_json),
    createdAt: Number(r.created_at),
    updatedAt: Number(r.updated_at),
  })));
});

const createBody = z.object({
  subjectId: z.string().nullable().optional(),
  title: z.string().max(80).optional(),
  strokes: strokesSchema.optional(),
});

notesRouter.post('/', async (req, res) => {
  const parsed = createBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = authed(req);
  const id = uuid();
  const now = Date.now();
  const strokes = parsed.data.strokes ?? [];
  const title = parsed.data.title ?? '';

  await db.run(
    'INSERT INTO notes (id, user_id, subject_id, title, strokes_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)',
    [id, userId, parsed.data.subjectId ?? null, title, JSON.stringify(strokes), now, now]
  );
  res.json({
    id, subjectId: parsed.data.subjectId ?? null, title, strokes,
    createdAt: now, updatedAt: now,
  });
});

const updateBody = z.object({
  title: z.string().max(80).optional(),
  subjectId: z.string().nullable().optional(),
  strokes: strokesSchema.optional(),
});

notesRouter.put('/:id', async (req, res) => {
  const parsed = updateBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = authed(req);

  const note = await db.get('SELECT id FROM notes WHERE id = ? AND user_id = ?', [req.params.id, userId]);
  if (!note) return res.status(404).json({ error: 'not_found' });

  const sets = ['updated_at = ?'];
  const vals: (string | number | null)[] = [Date.now()];
  if (parsed.data.title !== undefined) { sets.push('title = ?'); vals.push(parsed.data.title); }
  if (parsed.data.subjectId !== undefined) { sets.push('subject_id = ?'); vals.push(parsed.data.subjectId); }
  if (parsed.data.strokes !== undefined) { sets.push('strokes_json = ?'); vals.push(JSON.stringify(parsed.data.strokes)); }

  await db.run(`UPDATE notes SET ${sets.join(', ')} WHERE id = ?`, [...vals, req.params.id]);
  res.json({ ok: true });
});

notesRouter.delete('/:id', async (req, res) => {
  const { userId } = authed(req);
  const r = await db.run('DELETE FROM notes WHERE id = ? AND user_id = ?', [req.params.id, userId]);
  if (r.changes === 0) return res.status(404).json({ error: 'not_found' });
  res.json({ ok: true });
});
