import { db } from '../db/client.js';

const DAY = 24 * 60 * 60 * 1000;

export async function updateUserStatsOnComplete(userId: string, actualMin: number) {
  const now = Date.now();
  const user = await db.get<any>(
    'SELECT last_session_at, streak_days, total_focus_min FROM users WHERE id = ?',
    [userId]
  );
  if (!user) return;

  const last = user.last_session_at != null ? Number(user.last_session_at) : null;
  let streak = Number(user.streak_days);

  if (!last) {
    streak = 1;
  } else {
    const daysSince = Math.floor((now - last) / DAY);
    if (daysSince === 1) streak += 1;
    else if (daysSince > 1) streak = 1;
    // daysSince === 0 → same day, keep streak
  }

  const totalMin = Number(user.total_focus_min) + actualMin;
  const level = 1 + Math.floor(totalMin / 60); // one level per focused hour

  await db.run(
    'UPDATE users SET total_focus_min = ?, streak_days = ?, level = ?, last_session_at = ? WHERE id = ?',
    [totalMin, streak, level, now, userId]
  );
}

export async function addMinutesToSubject(subjectId: string | null | undefined, min: number) {
  if (!subjectId) return;
  await db.run('UPDATE subjects SET total_min = total_min + ? WHERE id = ?', [min, subjectId]);
}
