const BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? 'http://localhost:3000';

function getToken(): string | null {
  return localStorage.getItem('sg.jwt');
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');
  const token = getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const res = await fetch(`${BASE}${path}`, { ...init, headers });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`${res.status}: ${body}`);
  }
  return res.json() as Promise<T>;
}

export interface User {
  id: string;
  cosmicName: string;
  avatarSeed: string;
  level: number;
  totalFocusMin: number;
  streakDays: number;
  phoneNumber: string | null;
}

export interface Planet {
  id: string;
  name: string;
  color: string;
  totalMin: number;
}

export interface Star {
  id: string;
  subjectId: string | null;
  min: number;
  at: number;
  rating: number;
}

export interface BlackHole {
  id: string;
  subjectId: string | null;
  at: number;
}

export const api = {
  authAnonymous: (deviceId: string) =>
    request<{ jwt: string; user: User }>('/auth/anonymous', {
      method: 'POST',
      body: JSON.stringify({ deviceId }),
    }),
  signup: (phoneNumber: string, password: string, deviceId: string, cosmicName?: string) =>
    request<{ jwt: string; user: User }>('/auth/signup', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, password, deviceId, cosmicName }),
    }),
  login: (phoneNumber: string, password: string, deviceId: string) =>
    request<{ jwt: string; user: User }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, password, deviceId }),
    }),
  attachPhone: (phoneNumber: string, password: string) =>
    request<{ ok: true }>('/auth/attach-phone', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, password }),
    }),
  claimName: (name: string) =>
    request<{ ok: true; cosmicName: string }>('/auth/claim-name', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
  me: () => request<User>('/auth/me'),

  logout: () => {
    localStorage.removeItem('sg.jwt');
    localStorage.removeItem('sg.nameClaimed');
  },

  listSubjects: () => request<Planet[]>('/subjects'),
  createSubject: (name: string, color: string) =>
    request<Planet>('/subjects', { method: 'POST', body: JSON.stringify({ name, color }) }),

  startSession: (subjectId: string | null, plannedMin: number, intention: string) =>
    request<{ sessionId: string; serverStartTs: number }>('/sessions/start', {
      method: 'POST',
      body: JSON.stringify({ subjectId, plannedMin, intention }),
    }),
  completeSession: (sessionId: string, selfRating?: number) =>
    request<{ ok: true; actualMin: number }>('/sessions/complete', {
      method: 'POST',
      body: JSON.stringify({ sessionId, selfRating }),
    }),
  breakSession: (sessionId: string) =>
    request<{ ok: true }>('/sessions/break', {
      method: 'POST',
      body: JSON.stringify({ sessionId }),
    }),

  galaxy: () => request<{ planets: Planet[]; stars: Star[]; blackHoles: BlackHole[] }>('/galaxy'),

  listCircles: () => request<{ id: string; name: string; inviteCode: string }[]>('/circles'),
  createCircle: (name: string) =>
    request<{ id: string; name: string; inviteCode: string }>('/circles', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
  joinCircle: (inviteCode: string) =>
    request<{ id: string; name: string }>('/circles/join', {
      method: 'POST',
      body: JSON.stringify({ inviteCode }),
    }),

  statsWeekly: () =>
    request<{
      heatmap: { date: string; min: number }[];
      weekTotalMin: number;
      streakDays: number;
      totalFocusMin: number;
      level: number;
    }>('/stats/weekly'),

  startSessionWithTodos: (subjectId: string | null, plannedMin: number, intention: string, todos: string[]) =>
    request<{ sessionId: string; serverStartTs: number; todoIds: string[] }>('/sessions/start', {
      method: 'POST',
      body: JSON.stringify({
        subjectId,
        plannedMin,
        intention: intention || undefined,
        todos: todos.length ? todos.map(text => ({ text })) : undefined,
      }),
    }),
  completeSessionWithTodos: (sessionId: string, doneTodoIds: string[]) =>
    request<{ ok: true; actualMin: number }>('/sessions/complete', {
      method: 'POST',
      body: JSON.stringify({ sessionId, doneTodoIds }),
    }),

  listNotes: () => request<Note[]>('/notes'),
  createNote: (body: { subjectId: string | null; title: string; strokes: Stroke[] }) =>
    request<Note>('/notes', { method: 'POST', body: JSON.stringify(body) }),
  updateNote: (id: string, body: { title?: string; strokes?: Stroke[] }) =>
    request<{ ok: true }>(`/notes/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
};

export interface Stroke {
  color: string;
  width: number;
  erase?: boolean;
  points: [number, number][];
}
export interface Note {
  id: string;
  subjectId: string | null;
  title: string;
  strokes: Stroke[];
  createdAt: number;
  updatedAt: number;
}

export function storeToken(jwt: string) {
  localStorage.setItem('sg.jwt', jwt);
}
