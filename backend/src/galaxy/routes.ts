import { Router } from 'express';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';

export const galaxyRouter = Router();
galaxyRouter.use(requireAuth);

galaxyRouter.get('/', (req, res) => {
  const { userId } = req as AuthedRequest;

  const planets = db
    .prepare('SELECT id, name, color, total_min FROM subjects WHERE user_id = ? ORDER BY created_at')
    .all(userId) as any[];

  const sessions = db
    .prepare(`SELECT id, subject_id, started_at, actual_min, completed, broken_at, self_rating
              FROM sessions WHERE user_id = ? ORDER BY started_at`)
    .all(userId) as any[];

  const stars = sessions
    .filter(s => s.completed)
    .map(s => ({ id: s.id, subjectId: s.subject_id, min: s.actual_min, at: s.started_at, rating: s.self_rating ?? 3 }));

  const blackHoles = sessions
    .filter(s => s.broken_at)
    .map(s => ({ id: s.id, subjectId: s.subject_id, at: s.broken_at }));

  res.json({
    planets: planets.map(p => ({ id: p.id, name: p.name, color: p.color, totalMin: p.total_min })),
    stars,
    blackHoles,
  });
});
