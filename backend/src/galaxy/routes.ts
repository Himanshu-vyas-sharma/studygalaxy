import { Router } from 'express';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';

export const galaxyRouter = Router();
galaxyRouter.use(requireAuth);

galaxyRouter.get('/', async (req, res) => {
  const { userId } = req as AuthedRequest;

  const planets = await db.all<any>(
    'SELECT id, name, color, total_min FROM subjects WHERE user_id = ? ORDER BY created_at',
    [userId]
  );
  const sessions = await db.all<any>(
    `SELECT id, subject_id, started_at, actual_min, completed, broken_at, self_rating
     FROM sessions WHERE user_id = ? ORDER BY started_at`,
    [userId]
  );

  const stars = sessions
    .filter(s => Number(s.completed) === 1)
    .map(s => ({
      id: s.id,
      subjectId: s.subject_id,
      min: Number(s.actual_min ?? 0),
      at: Number(s.started_at),
      rating: s.self_rating == null ? 3 : Number(s.self_rating),
    }));

  const blackHoles = sessions
    .filter(s => s.broken_at != null)
    .map(s => ({ id: s.id, subjectId: s.subject_id, at: Number(s.broken_at) }));

  res.json({
    planets: planets.map(p => ({
      id: p.id, name: p.name, color: p.color, totalMin: Number(p.total_min),
    })),
    stars,
    blackHoles,
  });
});
