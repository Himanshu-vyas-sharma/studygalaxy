import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { requireAuth, type AuthedRequest } from '../auth/middleware.js';

export const profileRouter = Router();
profileRouter.use(requireAuth);

const profileBody = z.object({
  examGoal: z.enum(['boards', 'jee', 'neet', 'gate', 'upsc', 'cat', 'college', 'focus']).optional(),
  dailyHours: z.number().int().min(0).max(16).optional(),
  topSubjects: z.array(z.string().min(1).max(32)).max(10).optional(),
  studyTime: z.enum(['morning', 'afternoon', 'evening', 'night']).optional(),
  biggestDistraction: z.enum(['phone', 'social', 'noise', 'sleep', 'boredom', 'friends']).optional(),
});

profileRouter.post('/', async (req, res) => {
  const parsed = profileBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const p = parsed.data;

  const fields: string[] = [];
  const values: (string | number)[] = [];
  if (p.examGoal !== undefined) { fields.push('exam_goal = ?'); values.push(p.examGoal); }
  if (p.dailyHours !== undefined) { fields.push('daily_hours = ?'); values.push(p.dailyHours); }
  if (p.topSubjects !== undefined) { fields.push('top_subjects = ?'); values.push(JSON.stringify(p.topSubjects)); }
  if (p.studyTime !== undefined) { fields.push('study_time = ?'); values.push(p.studyTime); }
  if (p.biggestDistraction !== undefined) { fields.push('biggest_distraction = ?'); values.push(p.biggestDistraction); }
  if (fields.length === 0) return res.json({ ok: true });

  await db.run(`UPDATE users SET ${fields.join(', ')} WHERE id = ?`, [...values, userId]);
  res.json({ ok: true });
});

profileRouter.get('/', async (req, res) => {
  const { userId } = req as AuthedRequest;
  const u = await db.get<any>(
    'SELECT exam_goal, daily_hours, top_subjects, study_time, biggest_distraction FROM users WHERE id = ?',
    [userId]
  );
  if (!u) return res.status(404).json({ error: 'not_found' });
  res.json({
    examGoal: u.exam_goal,
    dailyHours: u.daily_hours == null ? null : Number(u.daily_hours),
    topSubjects: u.top_subjects ? JSON.parse(u.top_subjects) : null,
    studyTime: u.study_time,
    biggestDistraction: u.biggest_distraction,
  });
});
