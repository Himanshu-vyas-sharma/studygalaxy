import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Avatar } from '../../components/Avatar';
import { useStore } from '../../store';
import { api } from '../../data/api';

const COSMIC_QUOTES = [
  'The best time to study was yesterday. The second best is now.',
  'Every expert was once a star that refused to collapse.',
  'Discipline is choosing between what you want now and what you want most.',
  'Your galaxy is only as bright as the hours you give it.',
  'Focus is a muscle. Flex it.',
];

const QUICK_DURATIONS = [25, 45, 90];

export function HomeScreen() {
  const user = useStore(s => s.user)!;
  const setScreen = useStore(s => s.setScreen);
  const [intention, setIntention] = useState('');
  const [chosenMin, setChosenMin] = useState(25);
  const [starting, setStarting] = useState(false);
  const [globalCount, setGlobalCount] = useState(237);
  const quote = COSMIC_QUOTES[new Date().getDate() % COSMIC_QUOTES.length];

  // Simulated live global ticker — bumps every 4s like other students joined
  useEffect(() => {
    const id = setInterval(() => {
      setGlobalCount(c => c + (Math.random() < 0.6 ? 1 : -1));
    }, 4000);
    return () => clearInterval(id);
  }, []);

  const startFocus = async () => {
    setStarting(true);
    try {
      const { sessionId, serverStartTs } = await api.startSession(null, chosenMin, intention);
      useStore.getState().setActiveSession(sessionId, serverStartTs + chosenMin * 60_000, null);
      setScreen('timer');
    } catch (e) {
      console.error(e);
      setStarting(false);
    }
  };

  const totalHours = Math.floor(user.totalFocusMin / 60);
  const totalRemMin = user.totalFocusMin % 60;

  return (
    <div className="min-h-screen flex flex-col">
      {/* Right-now ticker */}
      <motion.div
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        className="pt-safe px-4 pt-3 text-center"
      >
        <span className="inline-flex items-center gap-2 text-[11px] tracking-widest uppercase text-violet-300/80">
          <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
          {globalCount} students focusing right now
        </span>
      </motion.div>

      {/* Top strip */}
      <div className="px-5 pt-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Avatar seed={user.avatarSeed} name={user.cosmicName} size={44} />
          <div>
            <div className="text-sm text-white/90 font-semibold">{user.cosmicName}</div>
            <div className="text-[11px] text-violet-300/80">Stargazer Lv.{user.level}</div>
          </div>
        </div>
        <div className="text-right">
          <div className="text-xs text-white/50">Streak</div>
          <div className="text-base font-semibold">
            <span className="mr-1">🔥</span>{user.streakDays}d
          </div>
        </div>
      </div>

      {/* Total hours vanity metric */}
      <div className="px-5 pt-6 text-center">
        <div className="text-xs tracking-widest text-white/40 uppercase">Total focused</div>
        <div className="font-display text-6xl gradient-text leading-none mt-1">
          {totalHours}<span className="text-3xl text-white/60">h {totalRemMin}m</span>
        </div>
      </div>

      {/* Hero portal */}
      <div className="flex-1 flex flex-col items-center justify-center px-5 relative">
        <motion.button
          onClick={startFocus}
          disabled={starting}
          whileTap={{ scale: 0.94 }}
          animate={{ scale: [1, 1.04, 1] }}
          transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
          className="glow-portal relative w-56 h-56 rounded-full bg-gradient-to-br from-violet-600 via-fuchsia-600 to-indigo-700 flex flex-col items-center justify-center text-center active:brightness-110 disabled:opacity-50"
        >
          <div className="absolute inset-3 rounded-full border border-white/20" />
          <div className="absolute inset-6 rounded-full border border-white/10" />
          <div className="text-xs tracking-[0.3em] text-white/70 uppercase">Tap to</div>
          <div className="font-display text-3xl mt-1">Focus</div>
          <div className="text-sm text-white/70 mt-1">{chosenMin} min</div>
        </motion.button>

        {/* Duration quick picks */}
        <div className="flex gap-2 mt-6">
          {QUICK_DURATIONS.map(m => (
            <button
              key={m}
              onClick={() => setChosenMin(m)}
              className={`px-4 py-2 rounded-full border text-sm transition ${
                chosenMin === m
                  ? 'bg-white/15 border-white/40'
                  : 'bg-white/5 border-white/10 hover:border-white/25'
              }`}
            >
              {m}m
            </button>
          ))}
        </div>

        {/* Intention */}
        <input
          value={intention}
          onChange={e => setIntention(e.target.value.slice(0, 100))}
          placeholder="What will you crush this session?"
          className="mt-5 w-full max-w-sm bg-white/5 border border-white/10 rounded-xl px-4 py-2.5 text-sm text-center placeholder:text-white/30 focus:outline-none focus:ring-2 focus:ring-violet-400/40"
        />

        {/* Live friends focusing */}
        <div className="mt-6 flex items-center gap-2 text-xs text-white/60">
          <div className="flex -space-x-2">
            {['Aarav', 'Riya', 'Zoya', 'Kabir'].map(n => (
              <Avatar key={n} seed={n} name={n} size={22} />
            ))}
          </div>
          <span>4 friends are focusing right now</span>
        </div>
      </div>

      {/* Social feed strip */}
      <div className="pb-6 pt-3">
        <div className="px-5 text-[11px] tracking-widest uppercase text-white/40 mb-2">Live feed</div>
        <div className="flex gap-3 px-5 overflow-x-auto scrollbar-none">
          {[
            { who: 'Riya', what: 'just finished 90m on Physics ⚛️' },
            { who: 'Arjun', what: "named a constellation: 'Midnight Grind'" },
            { who: 'IIT Prep Squad', what: '3 members focusing live' },
            { who: 'Zoya', what: 'Chemistry planet gained an atmosphere' },
          ].map((item, i) => (
            <div key={i} className="shrink-0 w-56 rounded-2xl bg-white/5 border border-white/10 p-3">
              <div className="text-sm font-semibold">{item.who}</div>
              <div className="text-xs text-white/60 mt-1">{item.what}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Bottom nav */}
      <div className="flex items-center justify-around pb-4 pt-2 border-t border-white/5">
        <NavBtn label="Home" active onClick={() => setScreen('home')} />
        <NavBtn label="Galaxy" onClick={() => setScreen('galaxy')} />
        <NavBtn label="Circles" onClick={() => setScreen('circles')} />
        <NavBtn label="Stats" onClick={() => setScreen('stats')} />
      </div>

      <div className="px-5 pb-safe pb-3 text-center text-[11px] italic text-white/35">
        "{quote}"
        <div className="mt-1 not-italic">
          <button onClick={() => setScreen('privacy')} className="underline underline-offset-2 text-white/40">Privacy</button>
        </div>
      </div>
    </div>
  );
}

function NavBtn({ label, active, onClick }: { label: string; active?: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`px-3 py-2 text-xs tracking-wide ${active ? 'text-white' : 'text-white/50'}`}
    >
      {label}
    </button>
  );
}
