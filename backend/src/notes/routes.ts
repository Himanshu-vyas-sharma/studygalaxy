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

notesRouter.get('/', (req, res) => {
  const { userId } = req as AuthedRequest;
  const rows = db
    .prepare('SELECT id, subject_id, title, strokes_json, created_at, updated_at FROM notes WHERE user_id = ? ORDER BY updated_at DESC')
    .all(userId) as any[];
  res.json(rows.map(r => ({
    id: r.id,
    subjectId: r.subject_id,
    title: r.title,
    strokes: JSON.parse(r.strokes_json),
    createdAt: r.created_at,
    updatedAt: r.updated_at,
  })));
});

const createBody = z.object({
  subjectId: z.string().nullable().optional(),
  title: z.string().max(80).optional(),
  strokes: strokesSchema.optional(),
});

notesRouter.post('/', (req, res) => {
  const parsed = createBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const id = uuid();
  const now = Date.now();
  db.prepare(
    'INSERT INTO notes (id, user_id, subject_id, title, strokes_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)'
  ).run(id, userId, parsed.data.subjectId ?? null, parsed.data.title ?? '', JSON.stringify(parsed.data.strokes ?? []), now, now);
  res.json({ id, subjectId: parsed.data.subjectId ?? null, title: parsed.data.title ?? '', strokes: parsed.data.strokes ?? [], createdAt: now, updatedAt: now });
});

const updateBody = z.object({
  title: z.string().max(80).optional(),
  subjectId: z.string().nullable().optional(),
  strokes: strokesSchema.optional(),
});

notesRouter.put('/:id', (req, res) => {
  const parsed = updateBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const note = db.prepare('SELECT id FROM notes WHERE id = ? AND user_id = ?').get(req.params.id, userId);
  if (!note) return res.status(404).json({ error: 'not_found' });
  const now = Date.now();
  const sets: string[] = ['updated_at = ?'];
  const vals: (string | number | null)[] = [now];
  if (parsed.data.title !== undefined) { sets.push('title = ?'); vals.push(parsed.data.title); }
  if (parsed.data.subjectId !== undefined) { sets.push('subject_id = ?'); vals.push(parsed.data.subjectId); }
  if (parsed.data.strokes !== undefined) { sets.push('strokes_json = ?'); vals.push(JSON.stringify(parsed.data.strokes)); }
  vals.push(req.params.id);
  db.prepare(`UPDATE notes SET ${sets.join(', ')} WHERE id = ?`).run(...vals);
  res.json({ ok: true });
});

notesRouter.delete('/:id', (req, res) => {
  const { userId } = req as AuthedRequest;
  const r = db.prepare('DELETE FROM notes WHERE id = ? AND user_id = ?').run(req.params.id, userId);
  if (r.changes === 0) return res.status(404).json({ error: 'not_found' });
  res.json({ ok: true });
});
