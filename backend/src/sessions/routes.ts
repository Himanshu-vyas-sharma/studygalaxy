import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';
import { uuid } from '../lib/uuid.js';
import { addMinutesToSubject, updateUserStatsOnComplete } from './service.js';
import { broadcastCircleEvent } from '../ws/events.js';

export const sessionsRouter = Router();
sessionsRouter.use(requireAuth);

const startBody = z.object({
  subjectId: z.string().nullable().optional(),
  plannedMin: z.number().int().min(1).max(300),
  intention: z.string().max(140).optional(),
  todos: z.array(z.object({ text: z.string().min(1).max(100) })).max(5).optional(),
});

sessionsRouter.post('/start', (req, res) => {
  const parsed = startBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const id = uuid();
  const now = Date.now();
  db.prepare(
    'INSERT INTO sessions (id, user_id, subject_id, started_at, planned_min, intention) VALUES (?, ?, ?, ?, ?, ?)'
  ).run(id, userId, parsed.data.subjectId ?? null, now, parsed.data.plannedMin, parsed.data.intention ?? null);

  const todos = parsed.data.todos ?? [];
  const todoIds: string[] = [];
  if (todos.length > 0) {
    const insert = db.prepare('INSERT INTO todos (id, session_id, text, ord) VALUES (?, ?, ?, ?)');
    const tx = db.transaction((items: { text: string }[]) => {
      items.forEach((t, i) => {
        const tid = uuid();
        insert.run(tid, id, t.text, i);
        todoIds.push(tid);
      });
    });
    tx(todos);
  }
  res.json({ sessionId: id, serverStartTs: now, todoIds });
});

const completeBody = z.object({
  sessionId: z.string(),
  selfRating: z.number().int().min(1).max(5).optional(),
  doneTodoIds: z.array(z.string()).max(5).optional(),
});

sessionsRouter.post('/complete', (req, res) => {
  const parsed = completeBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const sess = db.prepare('SELECT * FROM sessions WHERE id = ? AND user_id = ?').get(parsed.data.sessionId, userId) as any;
  if (!sess) return res.status(404).json({ error: 'no_session' });
  if (sess.completed || sess.broken_at) return res.status(409).json({ error: 'already_closed' });

  const now = Date.now();
  // Server-authoritative duration — floor to minutes, cap at planned
  const elapsedMin = Math.min(Math.floor((now - sess.started_at) / 60000), sess.planned_min);
  if (elapsedMin < 1) return res.status(400).json({ error: 'too_short' });

  // Mark todos done and derive self-rating from the completion ratio when client didn't send one
  let selfRating = parsed.data.selfRating ?? null;
  const totalTodos = (db.prepare('SELECT COUNT(*) as c FROM todos WHERE session_id = ?').get(sess.id) as any).c as number;
  if (totalTodos > 0) {
    const doneIds = parsed.data.doneTodoIds ?? [];
    if (doneIds.length > 0) {
      const placeholders = doneIds.map(() => '?').join(',');
      db.prepare(`UPDATE todos SET done = 1 WHERE session_id = ? AND id IN (${placeholders})`)
        .run(sess.id, ...doneIds);
    }
    const doneCount = (db.prepare('SELECT COUNT(*) as c FROM todos WHERE session_id = ? AND done = 1').get(sess.id) as any).c as number;
    if (selfRating == null) {
      const ratio = doneCount / totalTodos;
      selfRating = Math.max(1, Math.min(5, Math.round(1 + ratio * 4)));
    }
  }

  db.prepare('UPDATE sessions SET completed = 1, actual_min = ?, self_rating = ? WHERE id = ?')
    .run(elapsedMin, selfRating, sess.id);

  addMinutesToSubject(sess.subject_id, elapsedMin);
  updateUserStatsOnComplete(userId, elapsedMin);

  broadcastCircleEvent(userId, {
    type: 'session_complete',
    actualMin: elapsedMin,
    subjectId: sess.subject_id,
    at: now,
  });

  res.json({ ok: true, actualMin: elapsedMin });
});

const breakBody = z.object({ sessionId: z.string() });

sessionsRouter.post('/break', (req, res) => {
  const parsed = breakBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const sess = db.prepare('SELECT * FROM sessions WHERE id = ? AND user_id = ?').get(parsed.data.sessionId, userId) as any;
  if (!sess) return res.status(404).json({ error: 'no_session' });
  if (sess.completed || sess.broken_at) return res.status(409).json({ error: 'already_closed' });

  db.prepare('UPDATE sessions SET broken_at = ? WHERE id = ?').run(Date.now(), sess.id);

  broadcastCircleEvent(userId, { type: 'session_broken', subjectId: sess.subject_id, at: Date.now() });
  res.json({ ok: true });
});

sessionsRouter.get('/:id/todos', (req, res) => {
  const { userId } = req as AuthedRequest;
  const sess = db.prepare('SELECT id FROM sessions WHERE id = ? AND user_id = ?').get(req.params.id, userId);
  if (!sess) return res.status(404).json({ error: 'no_session' });
  const rows = db.prepare('SELECT id, text, done, ord FROM todos WHERE session_id = ? ORDER BY ord').all(req.params.id) as any[];
  res.json(rows.map(r => ({ id: r.id, text: r.text, done: !!r.done, ord: r.ord })));
});

sessionsRouter.get('/recent', (req, res) => {
  const { userId } = req as AuthedRequest;
  const rows = db
    .prepare('SELECT id, subject_id, started_at, planned_min, actual_min, completed, broken_at, intention, self_rating FROM sessions WHERE user_id = ? ORDER BY started_at DESC LIMIT 50')
    .all(userId) as any[];
  res.json(rows.map(r => ({
    id: r.id,
    subjectId: r.subject_id,
    startedAt: r.started_at,
    plannedMin: r.planned_min,
    actualMin: r.actual_min,
    completed: !!r.completed,
    brokenAt: r.broken_at,
    intention: r.intention,
    selfRating: r.self_rating,
  })));
});
