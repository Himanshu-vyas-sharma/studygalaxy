import express from 'express';
import cors from 'cors';
import { createServer } from 'node:http';
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

initSchema();

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

const httpServer = createServer(app);
attachWs(httpServer);

// Bind 0.0.0.0 so phones on the same WiFi can reach this over IPv4.
httpServer.listen(config.port, '0.0.0.0', () => {
  console.log(`✨ StudyGalaxy backend on http://0.0.0.0:${config.port}`);
});
