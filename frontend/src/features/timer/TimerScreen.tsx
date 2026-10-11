import { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { api } from '../../data/api';
import { useStore } from '../../store';
import { Confetti } from '../../components/Confetti';

function fmt(ms: number) {
  if (ms < 0) ms = 0;
  const totalSec = Math.floor(ms / 1000);
  const m = Math.floor(totalSec / 60).toString().padStart(2, '0');
  const s = (totalSec % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

export function TimerScreen() {
  const activeSessionId = useStore(s => s.activeSessionId);
  const endsAt = useStore(s => s.activeSessionEndsAt);
  const setScreen = useStore(s => s.setScreen);
  const setActiveSession = useStore(s => s.setActiveSession);
  const setUser = useStore(s => s.setUser);

  const [remaining, setRemaining] = useState(() => (endsAt ? endsAt - Date.now() : 0));
  const [warning, setWarning] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [brokeMessage, setBrokeMessage] = useState<string | null>(null);
  const leaveStartedAt = useRef<number | null>(null);

  useEffect(() => {
    if (!activeSessionId || !endsAt) return;
    const id = setInterval(() => {
      const r = endsAt - Date.now();
      setRemaining(r);
      if (r <= 0) {
        clearInterval(id);
        void finish();
      }
    }, 250);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSessionId, endsAt]);

  // Detect leaving the tab
  useEffect(() => {
    if (!activeSessionId) return;
    const onVis = () => {
      if (document.visibilityState === 'hidden') {
        leaveStartedAt.current = Date.now();
        setWarning(true);
      } else {
        const away = leaveStartedAt.current ? Date.now() - leaveStartedAt.current : 0;
        leaveStartedAt.current = null;
        if (away > 10_000) {
          void breakNow('You left for too long. Your star collapsed.');
        } else {
          setWarning(false);
        }
      }
    };
    document.addEventListener('visibilitychange', onVis);
    return () => document.removeEventListener('visibilitychange', onVis);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSessionId]);

  // Try to keep screen awake (ignored if unsupported)
  useEffect(() => {
    let lock: any = null;
    (async () => {
      try {
        // @ts-ignore - wakeLock typing not everywhere
        lock = await navigator.wakeLock?.request('screen');
      } catch { /* ignore */ }
    })();
    return () => { try { lock?.release(); } catch { /* ignore */ } };
  }, []);

  const finish = async () => {
    if (!activeSessionId || completed) return;
    setCompleted(true);
    try {
      const res = await api.completeSession(activeSessionId, 4);
      // refresh user stats
      try {
        const me = await api.me();
        setUser(me);
      } catch { /* ignore */ }
      setActiveSession(null, null, null);
      setTimeout(() => setScreen('galaxy'), 1800);
      void res;
    } catch (e) {
      console.error(e);
    }
  };

  const breakNow = async (msg: string) => {
    if (!activeSessionId) return;
    setBrokeMessage(msg);
    try {
      await api.breakSession(activeSessionId);
    } catch { /* ignore */ }
    setActiveSession(null, null, null);
    setTimeout(() => setScreen('home'), 2500);
  };

  if (!activeSessionId) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6 text-center">
        <div>
          <p className="text-white/70">No active session.</p>
          <button onClick={() => setScreen('home')} className="mt-4 px-4 py-2 rounded-full bg-white/10">Back home</button>
        </div>
      </div>
    );
  }

  if (completed) {
    return (
      <>
        <Confetti fire />
        <div className="min-h-screen flex flex-col items-center justify-center p-6">
          <motion.div
            initial={{ scale: 0.2, opacity: 0, rotate: -30 }}
            animate={{ scale: 1, opacity: 1, rotate: 0 }}
            transition={{ type: 'spring', stiffness: 160, damping: 12 }}
            className="relative w-40 h-40"
          >
            <motion.div
              animate={{ scale: [1, 1.15, 1] }}
              transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
              className="absolute inset-0 rounded-full bg-gradient-to-br from-amber-300 via-fuchsia-400 to-violet-500 glow-portal"
            />
            {[0, 1, 2, 3].map(i => (
              <motion.div
                key={i}
                animate={{ rotate: 360 }}
                transition={{ duration: 8 + i * 2, repeat: Infinity, ease: 'linear' }}
                className="absolute inset-0 flex items-center justify-center"
              >
                <div
                  className="absolute rounded-full bg-white"
                  style={{
                    width: 4,
                    height: 4,
                    transform: `translateX(${80 + i * 10}px)`,
                    opacity: 0.6 - i * 0.1,
                  }}
                />
              </motion.div>
            ))}
          </motion.div>
          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="font-display text-4xl mt-10 gradient-text"
          >
            A new star is born
          </motion.h2>
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.7 }}
            className="text-white/70 mt-2"
          >
            Your galaxy just grew.
          </motion.p>
        </div>
      </>
    );
  }

  if (brokeMessage) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-6 text-center">
        <motion.div
          initial={{ scale: 0, rotate: 0 }}
          animate={{ scale: 1, rotate: 360 }}
          transition={{ duration: 1.5 }}
          className="w-32 h-32 rounded-full bg-black border-4 border-violet-900 shadow-[0_0_80px_rgba(76,29,149,0.9)]"
        />
        <h2 className="font-display text-3xl mt-8 text-fuchsia-300">Black hole formed</h2>
        <p className="text-white/60 mt-2">{brokeMessage}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6">
      <div className="text-xs tracking-[0.4em] uppercase text-violet-300/70">Focusing</div>

      <motion.div
        animate={{ scale: warning ? [1, 1.08, 1] : 1 }}
        transition={{ duration: 0.6, repeat: warning ? Infinity : 0 }}
        className={`mt-4 w-64 h-64 rounded-full flex items-center justify-center ${
          warning
            ? 'bg-gradient-to-br from-red-600 via-fuchsia-700 to-violet-800'
            : 'bg-gradient-to-br from-violet-600 via-fuchsia-600 to-indigo-700'
        } glow-portal`}
      >
        <div className="font-display text-6xl tabular-nums">{fmt(remaining)}</div>
      </motion.div>

      {warning && (
        <p className="mt-6 text-sm text-red-300 text-center animate-pulse">
          Come back! Your star is about to collapse…
        </p>
      )}

      <button
        onClick={() => void breakNow('You gave up this session.')}
        className="mt-10 text-xs text-white/40 underline underline-offset-4"
      >
        Give up
      </button>
    </div>
  );
}
