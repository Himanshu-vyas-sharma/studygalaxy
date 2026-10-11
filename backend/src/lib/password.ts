import { scryptSync, randomBytes, timingSafeEqual } from 'node:crypto';

const N = 16384; // CPU cost
const r = 8;
const p = 1;
const keyLen = 64;
const saltLen = 16;

export function hashPassword(password: string): string {
  const salt = randomBytes(saltLen);
  const key = scryptSync(password, salt, keyLen, { N, r, p });
  return `scrypt$${N}$${r}$${p}$${salt.toString('base64')}$${key.toString('base64')}`;
}

export function verifyPassword(password: string, stored: string): boolean {
  try {
    const [scheme, nStr, rStr, pStr, saltB64, keyB64] = stored.split('$');
    if (scheme !== 'scrypt') return false;
    const salt = Buffer.from(saltB64, 'base64');
    const expected = Buffer.from(keyB64, 'base64');
    const actual = scryptSync(password, salt, expected.length, {
      N: Number(nStr),
      r: Number(rStr),
      p: Number(pStr),
    });
    return actual.length === expected.length && timingSafeEqual(actual, expected);
  } catch {
    return false;
  }
}
