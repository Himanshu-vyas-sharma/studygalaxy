import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';
import { uuid } from '../lib/uuid.js';
import { addMinutesToSubject, updateUserStatsOnComplete } from './service.js';
import { broadcastCircleEvent } from '../ws/events.js';

export const sessionsRouter = Router();
sessionsRouter.use(requireAuth);

const authed = (req: unknown) => req as AuthedRequest;

const startBody = z.object({
  subjectId: z.string().nullable().optional(),
  plannedMin: z.number().int().min(1).max(300),
  intention: z.string().max(140).optional(),
  todos: z.array(z.object({ text: z.string().min(1).max(100) })).max(5).optional(),
});

sessionsRouter.post('/start', async (req, res) => {
  const parsed = startBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const id = uuid();
  const now = Date.now();

  await db.run(
    'INSERT INTO sessions (id, user_id, subject_id, started_at, planned_min, intention) VALUES (?, ?, ?, ?, ?, ?)',
    [id, userId, parsed.data.subjectId ?? null, now, parsed.data.plannedMin, parsed.data.intention ?? null]
  );

  const todos = parsed.data.todos ?? [];
  const todoIds: string[] = [];
  if (todos.length > 0) {
    const stmts = todos.map((t, i) => {
      const tid = uuid();
      todoIds.push(tid);
      return { sql: 'INSERT INTO todos (id, session_id, text, ord) VALUES (?, ?, ?, ?)', args: [tid, id, t.text, i] };
    });
    await db.batch(stmts);
  }

  res.json({ sessionId: id, serverStartTs: now, todoIds });
});

const completeBody = z.object({
  sessionId: z.string(),
  selfRating: z.number().int().min(1).max(5).optional(),
  doneTodoIds: z.array(z.string()).max(5).optional(),
});

sessionsRouter.post('/complete', async (req, res) => {
  const parsed = completeBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;

  const sess = await db.get<any>('SELECT * FROM sessions WHERE id = ? AND user_id = ?', [
    parsed.data.sessionId, userId,
  ]);
  if (!sess) return res.status(404).json({ error: 'no_session' });
  if (Number(sess.completed) === 1 || sess.broken_at != null) {
    return res.status(409).json({ error: 'already_closed' });
  }

  const now = Date.now();
  // Server-authoritative duration — floor to minutes, cap at planned
  const elapsedMin = Math.min(
    Math.floor((now - Number(sess.started_at)) / 60000),
    Number(sess.planned_min)
  );
  if (elapsedMin < 1) return res.status(400).json({ error: 'too_short' });

  // Mark todos done; derive self-rating from completion ratio if client sent none.
  let selfRating: number | null = parsed.data.selfRating ?? null;
  const totalTodos = await db.count('SELECT COUNT(*) as c FROM todos WHERE session_id = ?', [sess.id]);
  if (totalTodos > 0) {
    const doneIds = parsed.data.doneTodoIds ?? [];
    if (doneIds.length > 0) {
      const placeholders = doneIds.map(() => '?').join(',');
      await db.run(
        `UPDATE todos SET done = 1 WHERE session_id = ? AND id IN (${placeholders})`,
        [sess.id, ...doneIds]
      );
    }
    const doneCount = await db.count(
      'SELECT COUNT(*) as c FROM todos WHERE session_id = ? AND done = 1', [sess.id]
    );
    if (selfRating == null) {
      selfRating = Math.max(1, Math.min(5, Math.round(1 + (doneCount / totalTodos) * 4)));
    }
  }

  await db.run('UPDATE sessions SET completed = 1, actual_min = ?, self_rating = ? WHERE id = ?', [
    elapsedMin, selfRating, sess.id,
  ]);

  await addMinutesToSubject(sess.subject_id, elapsedMin);
  await updateUserStatsOnComplete(userId, elapsedMin);
  await broadcastCircleEvent(userId, {
    type: 'session_complete', actualMin: elapsedMin, subjectId: sess.subject_id, at: now,
  });

  res.json({ ok: true, actualMin: elapsedMin });
});

const breakBody = z.object({ sessionId: z.string() });

sessionsRouter.post('/break', async (req, res) => {
  const parsed = breakBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;

  const sess = await db.get<any>('SELECT * FROM sessions WHERE id = ? AND user_id = ?', [
    parsed.data.sessionId, userId,
  ]);
  if (!sess) return res.status(404).json({ error: 'no_session' });
  if (Number(sess.completed) === 1 || sess.broken_at != null) {
    return res.status(409).json({ error: 'already_closed' });
  }

  const now = Date.now();
  await db.run('UPDATE sessions SET broken_at = ? WHERE id = ?', [now, sess.id]);
  await broadcastCircleEvent(userId, { type: 'session_broken', subjectId: sess.subject_id, at: now });
  res.json({ ok: true });
});

sessionsRouter.get('/:id/todos', async (req, res) => {
  const { userId } = authed(req);
  const sess = await db.get('SELECT id FROM sessions WHERE id = ? AND user_id = ?', [req.params.id, userId]);
  if (!sess) return res.status(404).json({ error: 'no_session' });
  const rows = await db.all<any>('SELECT id, text, done, ord FROM todos WHERE session_id = ? ORDER BY ord', [
    req.params.id,
  ]);
  res.json(rows.map(r => ({ id: r.id, text: r.text, done: Number(r.done) === 1, ord: Number(r.ord) })));
});

sessionsRouter.get('/recent', async (req, res) => {
  const { userId } = req as AuthedRequest;
  const rows = await db.all<any>(
    `SELECT id, subject_id, started_at, planned_min, actual_min, completed, broken_at, intention, self_rating
     FROM sessions WHERE user_id = ? ORDER BY started_at DESC LIMIT 50`,
    [userId]
  );
  res.json(rows.map(r => ({
    id: r.id,
    subjectId: r.subject_id,
    startedAt: Number(r.started_at),
    plannedMin: Number(r.planned_min),
    actualMin: r.actual_min == null ? null : Number(r.actual_min),
    completed: Number(r.completed) === 1,
    brokenAt: r.broken_at == null ? null : Number(r.broken_at),
    intention: r.intention,
    selfRating: r.self_rating == null ? null : Number(r.self_rating),
  })));
});
