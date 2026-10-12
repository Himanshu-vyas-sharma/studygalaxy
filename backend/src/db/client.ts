import { createClient, type Client, type InArgs } from '@libsql/client';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { config } from '../config.js';

// For a local file: URL make sure the folder exists before connecting.
if (config.databaseUrl.startsWith('file:')) {
  const path = config.databaseUrl.replace(/^file:/, '');
  mkdirSync(dirname(path), { recursive: true });
}

const client: Client = createClient({
  url: config.databaseUrl,
  authToken: config.databaseAuthToken,
});

export const isLocalFile = config.databaseUrl.startsWith('file:');

/** Thin async wrapper over libSQL with the shape the route handlers use. */
export const db = {
  /** First matching row, or undefined. */
  async get<T = Record<string, unknown>>(sql: string, params: InArgs = []): Promise<T | undefined> {
    const rs = await client.execute({ sql, args: params });
    return rs.rows[0] as T | undefined;
  },

  /** All matching rows. */
  async all<T = Record<string, unknown>>(sql: string, params: InArgs = []): Promise<T[]> {
    const rs = await client.execute({ sql, args: params });
    return rs.rows as unknown as T[];
  },

  /** Write; returns affected row count. */
  async run(sql: string, params: InArgs = []): Promise<{ changes: number }> {
    const rs = await client.execute({ sql, args: params });
    return { changes: Number(rs.rowsAffected ?? 0) };
  },

  /** Multi-statement DDL / migrations. */
  async exec(sql: string): Promise<void> {
    await client.executeMultiple(sql);
  },

  /** Atomic multi-write. */
  async batch(statements: { sql: string; args?: InArgs }[]): Promise<void> {
    if (statements.length === 0) return;
    await client.batch(
      statements.map(s => ({ sql: s.sql, args: s.args ?? [] })),
      'write'
    );
  },

  /** Single scalar COUNT(*) style helper. */
  async count(sql: string, params: InArgs = []): Promise<number> {
    const row = await db.get<{ c: number | bigint }>(sql, params);
    return Number(row?.c ?? 0);
  },
};

async function columnExists(table: string, col: string): Promise<boolean> {
  const rows = await db.all<{ name: string }>(`PRAGMA table_info(${table})`);
  return rows.some(r => r.name === col);
}

export async function initSchema(): Promise<void> {
  // Local file: tune SQLite for a single-node deploy. Turso manages this itself.
  if (isLocalFile) {
    for (const p of [
      'PRAGMA journal_mode = WAL',
      'PRAGMA synchronous = NORMAL',
      'PRAGMA busy_timeout = 5000',
      'PRAGMA cache_size = -64000',
      'PRAGMA temp_store = MEMORY',
    ]) {
      await client.execute(p).catch(() => {});
    }
  }
  await client.execute('PRAGMA foreign_keys = ON').catch(() => {});

  await db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      device_id TEXT UNIQUE NOT NULL,
      cosmic_name TEXT NOT NULL,
      avatar_seed TEXT NOT NULL,
      level INTEGER NOT NULL DEFAULT 1,
      total_focus_min INTEGER NOT NULL DEFAULT 0,
      streak_days INTEGER NOT NULL DEFAULT 0,
      last_session_at INTEGER,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS subjects (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      color TEXT NOT NULL,
      total_min INTEGER NOT NULL DEFAULT 0,
      planet_state TEXT NOT NULL DEFAULT '{}',
      created_at INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_subjects_user ON subjects(user_id);

    CREATE TABLE IF NOT EXISTS sessions (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      subject_id TEXT REFERENCES subjects(id) ON DELETE SET NULL,
      started_at INTEGER NOT NULL,
      planned_min INTEGER NOT NULL,
      actual_min INTEGER,
      completed INTEGER NOT NULL DEFAULT 0,
      broken_at INTEGER,
      intention TEXT,
      self_rating INTEGER
    );
    CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id);

    CREATE TABLE IF NOT EXISTS circles (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      invite_code TEXT UNIQUE NOT NULL,
      created_by TEXT NOT NULL REFERENCES users(id),
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS circle_members (
      circle_id TEXT NOT NULL REFERENCES circles(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      joined_at INTEGER NOT NULL,
      PRIMARY KEY (circle_id, user_id)
    );

    CREATE TABLE IF NOT EXISTS circle_events (
      id TEXT PRIMARY KEY,
      circle_id TEXT NOT NULL REFERENCES circles(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id),
      type TEXT NOT NULL,
      payload TEXT NOT NULL DEFAULT '{}',
      created_at INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_events_circle ON circle_events(circle_id, created_at DESC);

    CREATE TABLE IF NOT EXISTS notes (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      subject_id TEXT REFERENCES subjects(id) ON DELETE SET NULL,
      title TEXT NOT NULL DEFAULT '',
      strokes_json TEXT NOT NULL DEFAULT '[]',
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_notes_user ON notes(user_id, updated_at DESC);

    CREATE TABLE IF NOT EXISTS todos (
      id TEXT PRIMARY KEY,
      session_id TEXT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
      text TEXT NOT NULL,
      done INTEGER NOT NULL DEFAULT 0,
      ord INTEGER NOT NULL DEFAULT 0
    );
    CREATE INDEX IF NOT EXISTS idx_todos_session ON todos(session_id, ord);
  `);

  // Column migrations (safe to re-run)
  const adds: [string, string, string][] = [
    ['users', 'phone_number', 'TEXT'],
    ['users', 'password_hash', 'TEXT'],
    ['users', 'exam_goal', 'TEXT'],
    ['users', 'daily_hours', 'INTEGER'],
    ['users', 'top_subjects', 'TEXT'],
    ['users', 'study_time', 'TEXT'],
    ['users', 'biggest_distraction', 'TEXT'],
  ];
  for (const [table, col, type] of adds) {
    if (!(await columnExists(table, col))) {
      await db.run(`ALTER TABLE ${table} ADD COLUMN ${col} ${type}`);
    }
  }

  await db.exec(`
    CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number) WHERE phone_number IS NOT NULL;
    CREATE INDEX IF NOT EXISTS idx_sessions_user_started ON sessions(user_id, started_at DESC);
    CREATE INDEX IF NOT EXISTS idx_sessions_user_completed ON sessions(user_id, completed, started_at DESC);
    CREATE INDEX IF NOT EXISTS idx_members_user ON circle_members(user_id);
    CREATE INDEX IF NOT EXISTS idx_users_device ON users(device_id);
  `);

  await client.execute('ANALYZE').catch(() => {});
}
