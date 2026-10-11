import { useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { GalaxyBackground } from './components/GalaxyBackground';
import { useStore } from './store';
import { api, storeToken } from './data/api';
import { getOrCreateDeviceId } from './lib/uuid';
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

  useEffect(() => {
    (async () => {
      const deviceId = getOrCreateDeviceId();
      try {
        const existingToken = localStorage.getItem('sg.jwt');
        if (existingToken) {
          try {
            const me = await api.me();
            setUser(me);
            setScreen(localStorage.getItem('sg.nameClaimed') === '1' ? 'home' : 'namePicker');
            return;
          } catch { /* fall through to anon */ }
        }
        const { jwt, user } = await api.authAnonymous(deviceId);
        storeToken(jwt);
        setUser(user);
        setScreen('namePicker');
      } catch (e) {
        console.error('auth failed', e);
      }
    })();
  }, [setUser, setScreen]);

  // Mark name as claimed once we leave the name picker going into home
  useEffect(() => {
    if (screen === 'home') localStorage.setItem('sg.nameClaimed', '1');
  }, [screen]);

  return (
    <>
      <GalaxyBackground />
      <AnimatePresence mode="wait">
        <motion.div
          key={screen}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.3 }}
        >
          {screen === 'loading' && (
            <div className="min-h-screen flex items-center justify-center">
              <div className="font-display text-4xl gradient-text animate-pulse">StudyGalaxy</div>
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
