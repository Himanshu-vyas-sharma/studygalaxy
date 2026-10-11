import { randomUUID, randomBytes } from 'node:crypto';

export const uuid = () => randomUUID();

const ALPHA = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
export function inviteCode(len = 6): string {
  const bytes = randomBytes(len);
  let out = '';
  for (let i = 0; i < len; i++) out += ALPHA[bytes[i] % ALPHA.length];
  return out;
}

const COSMIC_PREFIX = ['Nebula', 'Quasar', 'Astro', 'Stellar', 'Orbit', 'Nova', 'Comet', 'Lunar', 'Solar', 'Cosmic', 'Pulsar', 'Galaxy'];
const COSMIC_SUFFIX = ['Ninja', 'Queen', 'King', 'Rider', 'Hunter', 'Wanderer', 'Dreamer', 'Scholar', 'Sage', 'Pilot', 'Spark', 'Wave'];

export function randomCosmicName(): string {
  const p = COSMIC_PREFIX[Math.floor(Math.random() * COSMIC_PREFIX.length)];
  const s = COSMIC_SUFFIX[Math.floor(Math.random() * COSMIC_SUFFIX.length)];
  return `${p}${s}`;
}
