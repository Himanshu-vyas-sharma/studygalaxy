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
import { attachWs } from './ws/server.js';

initSchema();

const app = express();
app.use(cors({ origin: config.corsOrigin }));
app.use(express.json({ limit: '64kb' }));

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

httpServer.listen(config.port, () => {
  console.log(`✨ StudyGalaxy backend on http://localhost:${config.port}`);
});
