import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { signJwt } from './jwt.js';
import { requireAuth, type AuthedRequest } from './middleware.js';
import { uuid, randomCosmicName } from '../lib/uuid.js';
import { rateLimit } from '../lib/rateLimit.js';

/**
 * StudyGalaxy has no passwords, no email and no phone numbers.
 *
 * An account is created from a device id the client generates on first run,
 * and is identified from then on by a JWT. Nothing personal is collected, so
 * there is nothing to sign in to and nothing to leak.
 *
 * The trade-off, which the UI states plainly: an account lives on the device
 * that made it. Android's Auto Backup carries the device id to a reinstall or
 * a new phone when the user has backup enabled, but that is best-effort.
 */
export const authRouter = Router();

// A device should not be able to mint endless accounts.
const anonLimiter = rateLimit({ windowMs: 60 * 60_000, max: 20 });

async function seedFirstLight(userId: string) {
  await db.run(
    'INSERT INTO subjects (id, user_id, name, color, created_at) VALUES (?, ?, ?, ?, ?)',
    [uuid(), userId, 'First Light', '#8B5CF6', Date.now()]
  );
}

function userToWire(u: any) {
  return {
    id: u.id,
    cosmicName: u.cosmic_name,
    avatarSeed: u.avatar_seed,
    level: Number(u.level),
    totalFocusMin: Number(u.total_focus_min),
    streakDays: Number(u.streak_days),
  };
}

// ---- The only way in ----
const anonBody = z.object({ deviceId: z.string().min(8).max(128) });

authRouter.post('/anonymous', anonLimiter, async (req, res) => {
  const parsed = anonBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { deviceId } = parsed.data;

  const existing = await db.get('SELECT * FROM users WHERE device_id = ?', [deviceId]);
  if (existing) {
    return res.json({ jwt: signJwt({ uid: (existing as any).id, did: deviceId }), user: userToWire(existing) });
  }

  const id = uuid();
  await db.run(
    'INSERT INTO users (id, device_id, cosmic_name, avatar_seed, created_at) VALUES (?, ?, ?, ?, ?)',
    [id, deviceId, randomCosmicName(), uuid().slice(0, 8), Date.now()]
  );
  await seedFirstLight(id);
  const u = await db.get('SELECT * FROM users WHERE id = ?', [id]);
  res.json({ jwt: signJwt({ uid: id, did: deviceId }), user: userToWire(u) });
});

// ---- Display name (not a credential) ----
const nameBody = z.object({ name: z.string().min(2).max(24).regex(/^[A-Za-z0-9 ]+$/) });

authRouter.post('/claim-name', requireAuth, async (req, res) => {
  const parsed = nameBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_name' });
  const { userId } = req as AuthedRequest;
  await db.run('UPDATE users SET cosmic_name = ? WHERE id = ?', [parsed.data.name, userId]);
  res.json({ ok: true, cosmicName: parsed.data.name });
});

authRouter.get('/me', requireAuth, async (req, res) => {
  const { userId } = req as AuthedRequest;
  const u = await db.get('SELECT * FROM users WHERE id = ?', [userId]);
  if (!u) return res.status(404).json({ error: 'not_found' });
  res.json(userToWire(u));
});
