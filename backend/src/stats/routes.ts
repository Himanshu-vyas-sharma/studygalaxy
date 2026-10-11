import { Router } from 'express';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';

export const statsRouter = Router();
statsRouter.use(requireAuth);

statsRouter.get('/weekly', (req, res) => {
  const { userId } = req as AuthedRequest;
  const now = Date.now();
  const DAY = 24 * 60 * 60 * 1000;
  const weekAgo = now - 7 * DAY;

  const sessions = db
    .prepare('SELECT started_at, actual_min FROM sessions WHERE user_id = ? AND completed = 1 AND started_at >= ?')
    .all(userId, weekAgo) as any[];

  const byDay: Record<string, number> = {};
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now - i * DAY).toISOString().slice(0, 10);
    byDay[d] = 0;
  }
  for (const s of sessions) {
    const key = new Date(s.started_at).toISOString().slice(0, 10);
    if (key in byDay) byDay[key] += s.actual_min ?? 0;
  }

  const user = db.prepare('SELECT streak_days, total_focus_min, level FROM users WHERE id = ?').get(userId) as any;
  const weekTotal = Object.values(byDay).reduce((a, b) => a + b, 0);

  res.json({
    heatmap: Object.entries(byDay).map(([date, min]) => ({ date, min })),
    weekTotalMin: weekTotal,
    streakDays: user?.streak_days ?? 0,
    totalFocusMin: user?.total_focus_min ?? 0,
    level: user?.level ?? 1,
  });
});
