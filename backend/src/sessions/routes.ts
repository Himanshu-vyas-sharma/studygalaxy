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
  res.json({ sessionId: id, serverStartTs: now });
});

const completeBody = z.object({ sessionId: z.string(), selfRating: z.number().int().min(1).max(5).optional() });

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

  db.prepare('UPDATE sessions SET completed = 1, actual_min = ?, self_rating = ? WHERE id = ?')
    .run(elapsedMin, parsed.data.selfRating ?? null, sess.id);

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
