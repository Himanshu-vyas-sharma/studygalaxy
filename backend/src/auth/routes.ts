import { Router } from 'express';
import { z } from 'zod';
import { db } from '../db/client.js';
import { signJwt } from './jwt.js';
import { requireAuth, type AuthedRequest } from './middleware.js';
import { uuid, randomCosmicName } from '../lib/uuid.js';
import { hashPassword, verifyPassword } from '../lib/password.js';
import { COUNTRIES, toE164, normalizeE164 } from '../lib/countries.js';
import { rateLimit } from '../lib/rateLimit.js';

export const authRouter = Router();

/**
 * Phone is always stored E.164: dial code + exactly 10 national digits.
 * Clients send { dialCode, phoneNumber } (preferred) or a full +<dial><10> string.
 */
function resolvePhone(input: { dialCode?: string; phoneNumber: string }): string | null {
  if (input.dialCode) return toE164(input.dialCode, input.phoneNumber);
  return normalizeE164(input.phoneNumber);
}

// Brute-force protection on the credential endpoints.
const authLimiter = rateLimit({ windowMs: 15 * 60_000, max: 30 });
const signupLimiter = rateLimit({ windowMs: 60 * 60_000, max: 10 });

authRouter.get('/countries', (_req, res) => res.json(COUNTRIES));

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
    phoneNumber: u.phone_number ?? null,
  };
}

// ---- Anonymous (guest) ----
const anonBody = z.object({ deviceId: z.string().min(8).max(128) });

authRouter.post('/anonymous', async (req, res) => {
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

// ---- Signup ----
const signupBody = z.object({
  dialCode: z.string().min(2).max(6).optional(),
  phoneNumber: z.string().min(10).max(20),
  password: z.string().min(8).max(72),
  deviceId: z.string().min(8).max(128),
  cosmicName: z.string().min(2).max(24).regex(/^[A-Za-z0-9 ]+$/).optional(),
});

authRouter.post('/signup', signupLimiter, async (req, res) => {
  const parsed = signupBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const phone = resolvePhone(parsed.data);
  if (!phone) return res.status(400).json({ error: 'bad_phone' });

  const existing = await db.get('SELECT id FROM users WHERE phone_number = ?', [phone]);
  if (existing) return res.status(409).json({ error: 'phone_taken' });

  const id = uuid();
  await db.run(
    `INSERT INTO users (id, device_id, cosmic_name, avatar_seed, phone_number, password_hash, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [
      id,
      parsed.data.deviceId,
      parsed.data.cosmicName ?? randomCosmicName(),
      uuid().slice(0, 8),
      phone,
      hashPassword(parsed.data.password),
      Date.now(),
    ]
  );
  await seedFirstLight(id);
  const u = await db.get('SELECT * FROM users WHERE id = ?', [id]);
  res.json({ jwt: signJwt({ uid: id, did: parsed.data.deviceId }), user: userToWire(u) });
});

// ---- Login ----
// New passwords must be 8+, but login stays at 6 so anyone who signed up
// under the old rule can still get into their account.
const loginBody = z.object({
  dialCode: z.string().min(2).max(6).optional(),
  phoneNumber: z.string().min(10).max(20),
  password: z.string().min(6).max(72),
  deviceId: z.string().min(8).max(128),
});

authRouter.post('/login', authLimiter, async (req, res) => {
  const parsed = loginBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const phone = resolvePhone(parsed.data);
  if (!phone) return res.status(401).json({ error: 'bad_credentials' });

  const u = (await db.get('SELECT * FROM users WHERE phone_number = ?', [phone])) as any;
  if (!u?.password_hash) return res.status(401).json({ error: 'bad_credentials' });
  if (!verifyPassword(parsed.data.password, u.password_hash)) {
    return res.status(401).json({ error: 'bad_credentials' });
  }
  await db.run('UPDATE users SET device_id = ? WHERE id = ?', [parsed.data.deviceId, u.id]);
  res.json({ jwt: signJwt({ uid: u.id, did: parsed.data.deviceId }), user: userToWire(u) });
});

// ---- Attach phone/password to an existing (anonymous) account ----
const attachBody = z.object({
  dialCode: z.string().min(2).max(6).optional(),
  phoneNumber: z.string().min(10).max(20),
  password: z.string().min(8).max(72),
});

authRouter.post('/attach-phone', requireAuth, async (req, res) => {
  const parsed = attachBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const phone = resolvePhone(parsed.data);
  if (!phone) return res.status(400).json({ error: 'bad_phone' });

  const taken = await db.get('SELECT id FROM users WHERE phone_number = ? AND id != ?', [phone, userId]);
  if (taken) return res.status(409).json({ error: 'phone_taken' });

  await db.run('UPDATE users SET phone_number = ?, password_hash = ? WHERE id = ?', [
    phone,
    hashPassword(parsed.data.password),
    userId,
  ]);
  res.json({ ok: true });
});

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
