import express from 'express';
import cors from 'cors';
import { createServer } from 'node:http';
import { existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { config } from './config.js';
import { initSchema } from './db/client.js';
import { authRouter } from './auth/routes.js';
import { subjectsRouter } from './subjects/routes.js';
import { sessionsRouter } from './sessions/routes.js';
import { circlesRouter } from './circles/routes.js';
import { galaxyRouter } from './galaxy/routes.js';
import { statsRouter } from './stats/routes.js';
import { profileRouter } from './profile/routes.js';
import { notesRouter } from './notes/routes.js';
import { rateLimit } from './lib/rateLimit.js';
import { attachWs } from './ws/server.js';

const app = express();
app.set('trust proxy', 1); // behind Render/Fly proxy so req.ip is the real client
app.use(cors({ origin: config.corsOrigin }));
// Notes carry stroke arrays, so allow a larger body than the default 100kb.
app.use(express.json({ limit: '2mb' }));

// Global safety net; per-route auth limits are stricter.
app.use(rateLimit({ windowMs: 60_000, max: 300 }));

app.get('/health', (_req, res) => res.json({ ok: true, service: 'studygalaxy' }));

app.use('/auth', authRouter);
app.use('/subjects', subjectsRouter);
app.use('/sessions', sessionsRouter);
app.use('/circles', circlesRouter);
app.use('/galaxy', galaxyRouter);
app.use('/stats', statsRouter);
app.use('/profile', profileRouter);
app.use('/notes', notesRouter);

/**
 * Serve the built web app from the same origin as the API, so one deploy hosts
 * the whole product — no separate static host and no CORS to configure.
 * Several candidates because the working directory differs between
 * `tsx src/index.ts` (dev) and `node dist/index.js` (prod).
 */
const here = dirname(fileURLToPath(import.meta.url));
const webRootCandidates = [
  join(here, '..', '..', 'frontend', 'dist'),      // backend/dist -> repo/frontend/dist
  join(here, '..', 'frontend', 'dist'),            // backend/src  -> repo/frontend/dist
  join(process.cwd(), '..', 'frontend', 'dist'),   // cwd = backend
  join(process.cwd(), 'frontend', 'dist'),         // cwd = repo root
];
const webRoot = webRootCandidates.find(p => existsSync(join(p, 'index.html')));

if (webRoot) {
  app.use(express.static(webRoot, { maxAge: '1h', index: false }));
  // SPA fallback so client-side routes resolve; API routes already matched above.
  app.get('*', (_req, res) => res.sendFile(join(webRoot, 'index.html')));
  console.log(`serving web app from ${webRoot}`);
} else {
  console.log('no frontend build found — API only. Looked in:');
  for (const c of webRootCandidates) console.log(`  ${c}`);
}

// Any route that throws returns JSON instead of an HTML stack trace.
app.use((err: unknown, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  console.error('[unhandled]', err);
  res.status(500).json({ error: 'server_error' });
});

const httpServer = createServer(app);
attachWs(httpServer);

// Schema must exist before we accept traffic.
await initSchema();

// Bind 0.0.0.0 so phones on the same WiFi can reach this over IPv4.
httpServer.listen(config.port, '0.0.0.0', () => {
  console.log(`✨ StudyGalaxy backend on http://0.0.0.0:${config.port}`);
});
