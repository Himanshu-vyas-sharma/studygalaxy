import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { signJwt } from './jwt.js';
import { requireAuth, type AuthedRequest } from './middleware.js';
import { uuid, randomCosmicName } from '../lib/uuid.js';

export const authRouter = Router();

const anonBody = z.object({ deviceId: z.string().min(8).max(128) });

authRouter.post('/anonymous', (req, res) => {
  const parsed = anonBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { deviceId } = parsed.data;

  const existing = db
    .prepare('SELECT id, cosmic_name, avatar_seed, level, total_focus_min, streak_days FROM users WHERE device_id = ?')
    .get(deviceId) as any;

  if (existing) {
    const jwt = signJwt({ uid: existing.id, did: deviceId });
    return res.json({
      jwt,
      user: {
        id: existing.id,
        cosmicName: existing.cosmic_name,
        avatarSeed: existing.avatar_seed,
        level: existing.level,
        totalFocusMin: existing.total_focus_min,
        streakDays: existing.streak_days,
      },
    });
  }

  const id = uuid();
  const cosmicName = randomCosmicName();
  const avatarSeed = uuid().slice(0, 8);
  db.prepare(
    `INSERT INTO users (id, device_id, cosmic_name, avatar_seed, created_at) VALUES (?, ?, ?, ?, ?)`
  ).run(id, deviceId, cosmicName, avatarSeed, Date.now());

  // Seed the "First Light" starter planet so the galaxy is never empty
  db.prepare(
    `INSERT INTO subjects (id, user_id, name, color, created_at) VALUES (?, ?, ?, ?, ?)`
  ).run(uuid(), id, 'First Light', '#8B5CF6', Date.now());

  const jwt = signJwt({ uid: id, did: deviceId });
  res.json({
    jwt,
    user: { id, cosmicName, avatarSeed, level: 1, totalFocusMin: 0, streakDays: 0 },
  });
});

const nameBody = z.object({ name: z.string().min(2).max(24).regex(/^[A-Za-z0-9 ]+$/) });

authRouter.post('/claim-name', requireAuth, (req, res) => {
  const parsed = nameBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_name' });
  const { userId } = req as AuthedRequest;
  db.prepare('UPDATE users SET cosmic_name = ? WHERE id = ?').run(parsed.data.name, userId);
  res.json({ ok: true, cosmicName: parsed.data.name });
});

authRouter.get('/me', requireAuth, (req, res) => {
  const { userId } = req as AuthedRequest;
  const u = db
    .prepare('SELECT id, cosmic_name, avatar_seed, level, total_focus_min, streak_days FROM users WHERE id = ?')
    .get(userId) as any;
  if (!u) return res.status(404).json({ error: 'not_found' });
  res.json({
    id: u.id,
    cosmicName: u.cosmic_name,
    avatarSeed: u.avatar_seed,
    level: u.level,
    totalFocusMin: u.total_focus_min,
    streakDays: u.streak_days,
  });
});
