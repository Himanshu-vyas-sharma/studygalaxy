import { useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { GalaxyBackground } from './components/GalaxyBackground';
import { useStore } from './store';
import { api, storeToken } from './data/api';
import { getOrCreateDeviceId } from './lib/uuid';
import { AuthLanding } from './features/auth/AuthLanding';
import { NamePicker } from './features/auth/NamePicker';
import { HomeScreen } from './features/home/HomeScreen';
import { TimerScreen } from './features/timer/TimerScreen';
import { GalaxyScreen } from './features/galaxy/GalaxyScreen';
import { CirclesScreen } from './features/circles/CirclesScreen';
import { StatsScreen } from './features/stats/StatsScreen';
import { PrivacyScreen } from './features/legal/PrivacyScreen';
import { SessionSetup } from './features/timer/SessionSetup';
import { SubjectPicker } from './features/subjects/SubjectPicker';
import { NotesScreen } from './features/notes/NotesScreen';
import { NoteCanvas } from './features/notes/NoteCanvas';

export function App() {
  const screen = useStore(s => s.screen);
  const setScreen = useStore(s => s.setScreen);
  const setUser = useStore(s => s.setUser);
  const user = useStore(s => s.user);

  // An invite link (/join/ABC123) parks the code until we have an account.
  useEffect(() => {
    const m = window.location.pathname.match(/^\/join\/([A-Za-z0-9]{4,10})\/?$/);
    if (m) {
      localStorage.setItem('sg.pendingInvite', m[1].toUpperCase());
      window.history.replaceState({}, '', '/');
    }
  }, []);

  // Once signed in (guest counts), redeem any parked invite.
  useEffect(() => {
    if (!user) return;
    const code = localStorage.getItem('sg.pendingInvite');
    if (!code) return;
    localStorage.removeItem('sg.pendingInvite');
    api.joinCircle(code)
      .then(() => setScreen('circles'))
      .catch(() => { /* bad or expired code — leave the user where they are */ });
  }, [user, setScreen]);

  // Bootstrap: if we have a token, try to resume. Else land on auth.
  useEffect(() => {
    (async () => {
      const existingToken = localStorage.getItem('sg.jwt');
      if (existingToken) {
        try {
          const me = await api.me();
          setUser(me);
          setScreen(localStorage.getItem('sg.nameClaimed') === '1' ? 'home' : 'namePicker');
          return;
        } catch {
          localStorage.removeItem('sg.jwt');
          localStorage.removeItem('sg.nameClaimed');
        }
      }
      setScreen('authLanding');
    })();
  }, [setUser, setScreen]);

  // When user picks "Continue as guest", mint an anonymous account on the fly
  useEffect(() => {
    if (screen !== 'guest') return;
    (async () => {
      try {
        const { jwt, user } = await api.authAnonymous(getOrCreateDeviceId());
        storeToken(jwt);
        setUser(user);
        setScreen('namePicker');
      } catch {
        setScreen('authLanding');
      }
    })();
  }, [screen, setUser, setScreen]);

  useEffect(() => {
    if (screen === 'home') localStorage.setItem('sg.nameClaimed', '1');
  }, [screen]);

  return (
    <>
      <GalaxyBackground />
      <AnimatePresence mode="wait">
        <motion.div
          key={screen}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -12 }}
          transition={{ duration: 0.35, ease: 'easeOut' }}
        >
          {screen === 'loading' && (
            <div className="min-h-screen flex items-center justify-center">
              <motion.div
                animate={{ scale: [1, 1.08, 1], opacity: [0.7, 1, 0.7] }}
                transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                className="font-display text-5xl gradient-text"
              >
                StudyGalaxy
              </motion.div>
            </div>
          )}
          {screen === 'authLanding' && <AuthLanding />}
          {screen === 'guest' && (
            <div className="min-h-screen flex items-center justify-center">
              <motion.div
                animate={{ scale: [1, 1.1, 1], opacity: [0.6, 1, 0.6] }}
                transition={{ duration: 1.4, repeat: Infinity }}
                className="font-display text-3xl gradient-text"
              >
                Entering galaxy…
              </motion.div>
            </div>
          )}
          {screen === 'namePicker' && user && <NamePicker />}
          {screen === 'home' && user && <HomeScreen />}
          {screen === 'timer' && <TimerScreen />}
          {screen === 'galaxy' && <GalaxyScreen />}
          {screen === 'circles' && <CirclesScreen />}
          {screen === 'stats' && <StatsScreen />}
          {screen === 'privacy' && <PrivacyScreen />}
          {screen === 'sessionSetup' && <SessionSetup />}
          {screen === 'subjectPicker' && <SubjectPicker />}
          {screen === 'notes' && <NotesScreen />}
          {screen === 'noteCanvas' && <NoteCanvas />}
        </motion.div>
      </AnimatePresence>
    </>
  );
}
