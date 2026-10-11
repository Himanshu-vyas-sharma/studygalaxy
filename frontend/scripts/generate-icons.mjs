// Generate PNG icons from SVG at required Play Store sizes.
// Run: npm run icons
import sharp from 'sharp';
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = resolve(__dirname, '..');
const outDir = resolve(root, 'public/icons');
mkdirSync(outDir, { recursive: true });

const SIZES = [192, 512];
const PLAY_STORE = 512;
const FEATURE_GRAPHIC = { w: 1024, h: 500, name: 'feature-graphic.png' };

const svg = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <defs>
    <radialGradient id="g" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#8B5CF6"/>
      <stop offset="60%" stop-color="#4C1D95"/>
      <stop offset="100%" stop-color="#05020D"/>
    </radialGradient>
  </defs>
  <rect width="512" height="512" fill="#05020D"/>
  <circle cx="256" cy="256" r="220" fill="url(#g)"/>
  <circle cx="256" cy="256" r="16" fill="#fff"/>
  <circle cx="380" cy="150" r="5" fill="#fff"/>
  <circle cx="130" cy="360" r="5" fill="#fff"/>
  <circle cx="410" cy="380" r="4" fill="#fff"/>
  <circle cx="110" cy="150" r="4" fill="#fff"/>
  <circle cx="300" cy="100" r="3" fill="#fff"/>
  <circle cx="200" cy="420" r="3" fill="#fff"/>
</svg>`;

const maskableSvg = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <defs>
    <radialGradient id="g" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#8B5CF6"/>
      <stop offset="60%" stop-color="#4C1D95"/>
      <stop offset="100%" stop-color="#05020D"/>
    </radialGradient>
  </defs>
  <rect width="512" height="512" fill="#05020D"/>
  <circle cx="256" cy="256" r="140" fill="url(#g)"/>
  <circle cx="256" cy="256" r="10" fill="#fff"/>
</svg>`;

for (const size of SIZES) {
  await sharp(Buffer.from(svg)).resize(size, size).png().toFile(`${outDir}/icon-${size}.png`);
  await sharp(Buffer.from(maskableSvg)).resize(size, size).png().toFile(`${outDir}/icon-maskable-${size}.png`);
  console.log(`icon-${size}.png + icon-maskable-${size}.png`);
}
await sharp(Buffer.from(svg)).resize(PLAY_STORE, PLAY_STORE).png().toFile(`${outDir}/play-store-icon-512.png`);
console.log('play-store-icon-512.png');

// Feature graphic (Play Store listing banner)
const feat = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 500">
  <defs>
    <radialGradient id="g" cx="50%" cy="60%" r="60%">
      <stop offset="0%" stop-color="#8B5CF6"/>
      <stop offset="50%" stop-color="#4C1D95"/>
      <stop offset="100%" stop-color="#05020D"/>
    </radialGradient>
  </defs>
  <rect width="1024" height="500" fill="url(#g)"/>
  <circle cx="200" cy="100" r="2" fill="#fff"/>
  <circle cx="800" cy="80" r="3" fill="#fff"/>
  <circle cx="900" cy="300" r="2" fill="#fff"/>
  <circle cx="150" cy="400" r="2" fill="#fff"/>
  <text x="512" y="240" font-family="serif" font-weight="900" font-size="88" fill="#fff" text-anchor="middle">StudyGalaxy</text>
  <text x="512" y="300" font-family="sans-serif" font-size="28" fill="#C4B5FD" text-anchor="middle">Focus with Friends. Grow your galaxy.</text>
</svg>`;
await sharp(Buffer.from(feat)).resize(FEATURE_GRAPHIC.w, FEATURE_GRAPHIC.h).png().toFile(`${outDir}/${FEATURE_GRAPHIC.name}`);
console.log(FEATURE_GRAPHIC.name);

console.log('\nDone. Icons in', outDir);
