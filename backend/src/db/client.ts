import Database from 'better-sqlite3';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { config } from '../config.js';

mkdirSync(dirname(config.dbPath), { recursive: true });

export const db = new Database(config.dbPath);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

function columnExists(table: string, col: string): boolean {
  const rows = db.prepare(`PRAGMA table_info(${table})`).all() as any[];
  return rows.some(r => r.name === col);
}

export function initSchema() {
  db.exec(`
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
  `);

  // Migrations for new auth columns (safe to run repeatedly)
  if (!columnExists('users', 'phone_number'))
    db.exec('ALTER TABLE users ADD COLUMN phone_number TEXT');
  if (!columnExists('users', 'password_hash'))
    db.exec('ALTER TABLE users ADD COLUMN password_hash TEXT');
  db.exec('CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number) WHERE phone_number IS NOT NULL');

  // Onboarding answers
  if (!columnExists('users', 'exam_goal')) db.exec('ALTER TABLE users ADD COLUMN exam_goal TEXT');
  if (!columnExists('users', 'daily_hours')) db.exec('ALTER TABLE users ADD COLUMN daily_hours INTEGER');
  if (!columnExists('users', 'top_subjects')) db.exec('ALTER TABLE users ADD COLUMN top_subjects TEXT');
  if (!columnExists('users', 'study_time')) db.exec('ALTER TABLE users ADD COLUMN study_time TEXT');
  if (!columnExists('users', 'biggest_distraction')) db.exec('ALTER TABLE users ADD COLUMN biggest_distraction TEXT');

  // Notes
  db.exec(`
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
  `);

  // Session-linked todos
  db.exec(`
    CREATE TABLE IF NOT EXISTS todos (
      id TEXT PRIMARY KEY,
      session_id TEXT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
      text TEXT NOT NULL,
      done INTEGER NOT NULL DEFAULT 0,
      ord INTEGER NOT NULL DEFAULT 0
    );
    CREATE INDEX IF NOT EXISTS idx_todos_session ON todos(session_id, ord);
  `);
}
