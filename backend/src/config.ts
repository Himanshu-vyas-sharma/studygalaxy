import 'dotenv/config';

/**
 * DATABASE_URL accepts either form:
 *   file:./data/studygalaxy.sqlite   — local dev, plain SQLite file
 *   libsql://<db>.turso.io          — hosted Turso (set DATABASE_AUTH_TOKEN too)
 */
export const config = {
  port: Number(process.env.PORT ?? 3000),
  jwtSecret: process.env.JWT_SECRET ?? 'dev-secret-change-me',
  databaseUrl: process.env.DATABASE_URL ?? 'file:./data/studygalaxy.sqlite',
  databaseAuthToken: process.env.DATABASE_AUTH_TOKEN || undefined,
  corsOrigin: process.env.CORS_ORIGIN ?? '*',
};
