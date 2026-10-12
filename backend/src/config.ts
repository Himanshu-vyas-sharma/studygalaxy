import 'dotenv/config';

export const config = {
  port: Number(process.env.PORT ?? 3000),
  jwtSecret: process.env.JWT_SECRET ?? 'dev-secret-change-me',
  dbPath: process.env.DB_PATH ?? './data/studygalaxy.sqlite',
  corsOrigin: process.env.CORS_ORIGIN ?? '*',
};
