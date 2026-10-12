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

function seedFirstLight(userId: string) {
  db.prepare(
    `INSERT INTO subjects (id, user_id, name, color, created_at) VALUES (?, ?, ?, ?, ?)`
  ).run(uuid(), userId, 'First Light', '#8B5CF6', Date.now());
}

function userToWire(u: any) {
  return {
    id: u.id,
    cosmicName: u.cosmic_name,
    avatarSeed: u.avatar_seed,
    level: u.level,
    totalFocusMin: u.total_focus_min,
    streakDays: u.streak_days,
    phoneNumber: u.phone_number ?? null,
  };
}

// ---- Anonymous (guest) ----
const anonBody = z.object({ deviceId: z.string().min(8).max(128) });

authRouter.post('/anonymous', (req, res) => {
  const parsed = anonBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { deviceId } = parsed.data;

  const existing = db.prepare('SELECT * FROM users WHERE device_id = ?').get(deviceId) as any;
  if (existing) {
    const jwt = signJwt({ uid: existing.id, did: deviceId });
    return res.json({ jwt, user: userToWire(existing) });
  }

  const id = uuid();
  const cosmicName = randomCosmicName();
  const avatarSeed = uuid().slice(0, 8);
  db.prepare(
    `INSERT INTO users (id, device_id, cosmic_name, avatar_seed, created_at) VALUES (?, ?, ?, ?, ?)`
  ).run(id, deviceId, cosmicName, avatarSeed, Date.now());
  seedFirstLight(id);
  const jwt = signJwt({ uid: id, did: deviceId });
  const u = db.prepare('SELECT * FROM users WHERE id = ?').get(id);
  res.json({ jwt, user: userToWire(u) });
});

// ---- Signup with phone + password ----
const signupBody = z.object({
  dialCode: z.string().min(2).max(6).optional(),
  phoneNumber: z.string().min(10).max(20),
  password: z.string().min(6).max(72),
  deviceId: z.string().min(8).max(128),
  cosmicName: z.string().min(2).max(24).regex(/^[A-Za-z0-9 ]+$/).optional(),
});

authRouter.post('/signup', signupLimiter, (req, res) => {
  const parsed = signupBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const phone = resolvePhone(parsed.data);
  if (!phone) return res.status(400).json({ error: 'bad_phone' });

  const existing = db.prepare('SELECT id FROM users WHERE phone_number = ?').get(phone);
  if (existing) return res.status(409).json({ error: 'phone_taken' });

  const id = uuid();
  const cosmicName = parsed.data.cosmicName ?? randomCosmicName();
  const avatarSeed = uuid().slice(0, 8);
  const passwordHash = hashPassword(parsed.data.password);

  db.prepare(
    `INSERT INTO users (id, device_id, cosmic_name, avatar_seed, phone_number, password_hash, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).run(id, parsed.data.deviceId, cosmicName, avatarSeed, phone, passwordHash, Date.now());
  seedFirstLight(id);

  const jwt = signJwt({ uid: id, did: parsed.data.deviceId });
  const u = db.prepare('SELECT * FROM users WHERE id = ?').get(id);
  res.json({ jwt, user: userToWire(u) });
});

// ---- Login with phone + password ----
const loginBody = z.object({
  dialCode: z.string().min(2).max(6).optional(),
  phoneNumber: z.string().min(10).max(20),
  password: z.string().min(6).max(72),
  deviceId: z.string().min(8).max(128),
});

authRouter.post('/login', authLimiter, (req, res) => {
  const parsed = loginBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const phone = resolvePhone(parsed.data);
  if (!phone) return res.status(401).json({ error: 'bad_credentials' });

  const u = db.prepare('SELECT * FROM users WHERE phone_number = ?').get(phone) as any;
  if (!u || !u.password_hash) return res.status(401).json({ error: 'bad_credentials' });
  if (!verifyPassword(parsed.data.password, u.password_hash)) {
    return res.status(401).json({ error: 'bad_credentials' });
  }
  // Update device_id in case they switched phones
  db.prepare('UPDATE users SET device_id = ? WHERE id = ?').run(parsed.data.deviceId, u.id);
  const jwt = signJwt({ uid: u.id, did: parsed.data.deviceId });
  res.json({ jwt, user: userToWire(u) });
});

// ---- Attach phone/password to an existing (anonymous) account ----
const attachBody = z.object({
  dialCode: z.string().min(2).max(6).optional(),
  phoneNumber: z.string().min(10).max(20),
  password: z.string().min(6).max(72),
});

authRouter.post('/attach-phone', requireAuth, (req, res) => {
  const parsed = attachBody.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: 'bad_body' });
  const { userId } = req as AuthedRequest;
  const phone = resolvePhone(parsed.data);
  if (!phone) return res.status(400).json({ error: 'bad_phone' });

  const taken = db.prepare('SELECT id FROM users WHERE phone_number = ? AND id != ?').get(phone, userId);
  if (taken) return res.status(409).json({ error: 'phone_taken' });

  db.prepare('UPDATE users SET phone_number = ?, password_hash = ? WHERE id = ?')
    .run(phone, hashPassword(parsed.data.password), userId);
  res.json({ ok: true });
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
  const u = db.prepare('SELECT * FROM users WHERE id = ?').get(userId) as any;
  if (!u) return res.status(404).json({ error: 'not_found' });
  res.json(userToWire(u));
});
