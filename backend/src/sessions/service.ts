import { db } from '../db/client.js';

export function updateUserStatsOnComplete(userId: string, actualMin: number) {
  const now = Date.now();
  const user = db.prepare('SELECT last_session_at, streak_days, total_focus_min FROM users WHERE id = ?').get(userId) as any;
  if (!user) return;

  const DAY = 24 * 60 * 60 * 1000;
  const last = user.last_session_at as number | null;
  let streak = user.streak_days as number;

  if (!last) streak = 1;
  else {
    const daysSince = Math.floor((now - last) / DAY);
    if (daysSince === 0) {
      // same day, keep streak
    } else if (daysSince === 1) {
      streak += 1;
    } else {
      streak = 1;
    }
  }

  const totalMin = user.total_focus_min + actualMin;
  const level = 1 + Math.floor(totalMin / 60); // 1 level per focused hour

  db.prepare(
    'UPDATE users SET total_focus_min = ?, streak_days = ?, level = ?, last_session_at = ? WHERE id = ?'
  ).run(totalMin, streak, level, now, userId);
}

export function addMinutesToSubject(subjectId: string | null | undefined, min: number) {
  if (!subjectId) return;
  db.prepare('UPDATE subjects SET total_min = total_min + ? WHERE id = ?').run(min, subjectId);
}
