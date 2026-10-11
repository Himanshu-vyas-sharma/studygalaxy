import { create } from 'zustand';
import type { User } from './data/api';

type Screen =
  | 'loading'
  | 'authLanding'
  | 'signup'
  | 'login'
  | 'guest'
  | 'namePicker'
  | 'home'
  | 'timer'
  | 'galaxy'
  | 'circles'
  | 'stats'
  | 'privacy';

interface Store {
  user: User | null;
  screen: Screen;
  activeSessionId: string | null;
  activeSessionEndsAt: number | null;
  activeSubjectId: string | null;
  setUser: (u: User | null) => void;
  setScreen: (s: Screen) => void;
  setActiveSession: (id: string | null, endsAt: number | null, subjectId: string | null) => void;
}

export const useStore = create<Store>(set => ({
  user: null,
  screen: 'loading',
  activeSessionId: null,
  activeSessionEndsAt: null,
  activeSubjectId: null,
  setUser: user => set({ user }),
  setScreen: screen => set({ screen }),
  setActiveSession: (activeSessionId, activeSessionEndsAt, activeSubjectId) =>
    set({ activeSessionId, activeSessionEndsAt, activeSubjectId }),
}));
