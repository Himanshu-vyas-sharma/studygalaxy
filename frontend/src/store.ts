import { create } from 'zustand';
import type { User } from './data/api';

type Screen =
  | 'loading'
  | 'authLanding'
  | 'guest'
  | 'namePicker'
  | 'home'
  | 'sessionSetup'
  | 'subjectPicker'
  | 'timer'
  | 'galaxy'
  | 'circles'
  | 'notes'
  | 'noteCanvas'
  | 'stats'
  | 'privacy';

export interface TimerTodo { id: string; text: string; done: boolean }

interface Store {
  user: User | null;
  screen: Screen;
  activeSessionId: string | null;
  activeSessionEndsAt: number | null;
  activeSubjectId: string | null;
  activeTodos: TimerTodo[];
  editingNoteId: string | null;
  setUser: (u: User | null) => void;
  setScreen: (s: Screen) => void;
  setActiveSession: (id: string | null, endsAt: number | null, subjectId: string | null) => void;
  setActiveTodos: (todos: TimerTodo[]) => void;
  toggleTodo: (id: string) => void;
  setActiveSubject: (id: string | null) => void;
  setEditingNote: (id: string | null) => void;
}

export const useStore = create<Store>(set => ({
  user: null,
  screen: 'loading',
  activeSessionId: null,
  activeSessionEndsAt: null,
  activeSubjectId: null,
  activeTodos: [],
  editingNoteId: null,
  setUser: user => set({ user }),
  setScreen: screen => set({ screen }),
  setActiveSession: (activeSessionId, activeSessionEndsAt, activeSubjectId) =>
    set({ activeSessionId, activeSessionEndsAt, activeSubjectId }),
  setActiveTodos: activeTodos => set({ activeTodos }),
  toggleTodo: id => set(s => ({ activeTodos: s.activeTodos.map(t => t.id === id ? { ...t, done: !t.done } : t) })),
  setActiveSubject: id => set({ activeSubjectId: id }),
  setEditingNote: editingNoteId => set({ editingNoteId }),
}));
