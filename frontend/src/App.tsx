import { useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { GalaxyBackground } from './components/GalaxyBackground';
import { useStore } from './store';
import { api, storeToken } from './data/api';
import { getOrCreateDeviceId } from './lib/uuid';
import { AuthLanding } from './features/auth/AuthLanding';
import { SignupScreen } from './features/auth/SignupScreen';
import { LoginScreen } from './features/auth/LoginScreen';
import { NamePicker } from './features/auth/NamePicker';
import { HomeScreen } from './features/home/HomeScreen';
import { TimerScreen } from './features/timer/TimerScreen';
import { GalaxyScreen } from './features/galaxy/GalaxyScreen';
import { CirclesScreen } from './features/circles/CirclesScreen';
import { StatsScreen } from './features/stats/StatsScreen';
import { PrivacyScreen } from './features/legal/PrivacyScreen';

export function App() {
  const screen = useStore(s => s.screen);
  const setScreen = useStore(s => s.setScreen);
  const setUser = useStore(s => s.setUser);
  const user = useStore(s => s.user);

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
          {screen === 'signup' && <SignupScreen />}
          {screen === 'login' && <LoginScreen />}
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
        </motion.div>
      </AnimatePresence>
    </>
  );
}
