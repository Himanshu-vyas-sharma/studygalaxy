import { useState } from 'react';
import { motion } from 'framer-motion';
import { useStore } from '../../store';
import { api, storeToken } from '../../data/api';
import { getOrCreateDeviceId } from '../../lib/uuid';

export function AuthLanding() {
  const setScreen = useStore(s => s.setScreen);
  const setUser = useStore(s => s.setUser);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // Zero-friction entry: no email, no phone, no password.
  const startAsGuest = async () => {
    setBusy(true);
    setErr(null);
    try {
      const { jwt, user } = await api.authAnonymous(getOrCreateDeviceId());
      storeToken(jwt);
      setUser(user);
      setScreen('namePicker');
    } catch {
      setErr("Couldn't reach StudyGalaxy. Check your connection.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-6 relative">
      <motion.div
        initial={{ opacity: 0, y: 30, scale: 0.8 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.9, ease: 'easeOut' }}
        className="w-full max-w-sm text-center"
      >
        <motion.div
          animate={{ y: [0, -8, 0], rotate: [0, 3, 0] }}
          transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
          className="w-28 h-28 mx-auto rounded-full bg-gradient-to-br from-violet-500 via-fuchsia-500 to-indigo-600 glow-portal mb-8"
        />
        <p className="text-xs tracking-[0.5em] text-violet-300/70 uppercase mb-3">Welcome to</p>
        <motion.h1
          initial={{ letterSpacing: '0.3em', opacity: 0 }}
          animate={{ letterSpacing: '0em', opacity: 1 }}
          transition={{ delay: 0.4, duration: 1 }}
          className="font-display text-6xl gradient-text mb-4"
        >
          StudyGalaxy
        </motion.h1>
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1 }}
          className="text-white/70 mb-10"
        >
          Focus. Grow your galaxy. Study with friends.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.3 }}
          className="space-y-3"
        >
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.95 }}
            disabled={busy}
            onClick={startAsGuest}
            className="w-full py-4 rounded-2xl font-semibold text-lg bg-gradient-to-r from-violet-500 to-fuchsia-500 shadow-lg shadow-violet-500/30 disabled:opacity-60"
          >
            {busy ? 'Opening your galaxy…' : 'Start focusing'}
          </motion.button>
          <p className="text-xs text-white/50">No email. No phone. No password.</p>

          {err && <p className="text-xs text-red-300">{err}</p>}

          <p className="text-xs text-white/50 pt-4">Want your galaxy on more than one device?</p>
          <div className="flex gap-3">
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setScreen('signup')}
              className="flex-1 py-3 rounded-2xl text-sm bg-white/10 border border-white/15"
            >
              Create account
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setScreen('login')}
              className="flex-1 py-3 rounded-2xl text-sm bg-white/10 border border-white/15"
            >
              Log in
            </motion.button>
          </div>
        </motion.div>
      </motion.div>
    </div>
  );
}
