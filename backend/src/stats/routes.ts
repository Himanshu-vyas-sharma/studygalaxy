import { Router } from 'express';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';

export const statsRouter = Router();
statsRouter.use(requireAuth);

const DAY = 24 * 60 * 60 * 1000;

statsRouter.get('/weekly', async (req, res) => {
  const { userId } = req as AuthedRequest;
  const now = Date.now();
  const weekAgo = now - 7 * DAY;

  const sessions = await db.all<any>(
    'SELECT started_at, actual_min FROM sessions WHERE user_id = ? AND completed = 1 AND started_at >= ?',
    [userId, weekAgo]
  );

  const byDay: Record<string, number> = {};
  for (let i = 6; i >= 0; i--) {
    byDay[new Date(now - i * DAY).toISOString().slice(0, 10)] = 0;
  }
  for (const s of sessions) {
    const key = new Date(Number(s.started_at)).toISOString().slice(0, 10);
    if (key in byDay) byDay[key] += Number(s.actual_min ?? 0);
  }

  const user = await db.get<any>('SELECT streak_days, total_focus_min, level FROM users WHERE id = ?', [userId]);
  const weekTotal = Object.values(byDay).reduce((a, b) => a + b, 0);

  res.json({
    heatmap: Object.entries(byDay).map(([date, min]) => ({ date, min })),
    weekTotalMin: weekTotal,
    streakDays: Number(user?.streak_days ?? 0),
    totalFocusMin: Number(user?.total_focus_min ?? 0),
    level: Number(user?.level ?? 1),
  });
});
