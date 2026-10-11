import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Avatar } from '../../components/Avatar';
import { AnimatedNumber } from '../../components/AnimatedNumber';
import { useStore } from '../../store';
import { api, type Planet } from '../../data/api';

const COSMIC_QUOTES = [
  'The best time to study was yesterday. The second best is now.',
  'Every expert was once a star that refused to collapse.',
  'Discipline is choosing between what you want now and what you want most.',
  'Your galaxy is only as bright as the hours you give it.',
  'Focus is a muscle. Flex it.',
];

export function HomeScreen() {
  const user = useStore(s => s.user)!;
  const setScreen = useStore(s => s.setScreen);
  const activeSubjectId = useStore(s => s.activeSubjectId);
  const setActiveSubject = useStore(s => s.setActiveSubject);
  const [subjects, setSubjects] = useState<Planet[]>([]);
  const quote = COSMIC_QUOTES[new Date().getDate() % COSMIC_QUOTES.length];

  useEffect(() => {
    api.listSubjects().then(subs => {
      setSubjects(subs);
      if (!activeSubjectId && subs.length) setActiveSubject(subs[0].id);
    }).catch(() => {});
  }, []);

  const activeSubject = subjects.find(s => s.id === activeSubjectId);
  const totalHours = Math.floor(user.totalFocusMin / 60);
  const totalRemMin = user.totalFocusMin % 60;

  return (
    <div className="min-h-screen flex flex-col">
      {/* Top strip */}
      <div className="px-5 pt-6 flex items-center justify-between">
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

      {/* Total hours */}
      <motion.div
        initial={{ opacity: 0, y: 20, scale: 0.95 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ delay: 0.1, duration: 0.5 }}
        className="px-5 pt-8 text-center"
      >
        <div className="text-xs tracking-widest text-white/40 uppercase">Total focused</div>
        <div className="font-display text-6xl gradient-text leading-none mt-1">
          <AnimatedNumber value={totalHours} />
          <span className="text-3xl text-white/60">h <AnimatedNumber value={totalRemMin} />m</span>
        </div>
      </motion.div>

      {/* Hero portal */}
      <div className="flex-1 flex flex-col items-center justify-center px-5">
        <div className="relative">
          {[0, 1, 2].map(i => (
            <motion.div
              key={i}
              className="absolute inset-0 flex items-center justify-center pointer-events-none"
              animate={{ rotate: 360 }}
              transition={{ duration: 10 + i * 4, repeat: Infinity, ease: 'linear' }}
            >
              <div
                className="absolute rounded-full"
                style={{
                  width: 6 - i,
                  height: 6 - i,
                  background: ['#C4B5FD', '#F472B6', '#FBBF24'][i],
                  transform: `translateX(${130 + i * 8}px)`,
                  boxShadow: `0 0 8px ${['#C4B5FD', '#F472B6', '#FBBF24'][i]}`,
                }}
              />
            </motion.div>
          ))}
          <motion.button
            onClick={() => setScreen('sessionSetup')}
            whileTap={{ scale: 0.94 }}
            animate={{ scale: [1, 1.04, 1] }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
            className="glow-portal relative w-56 h-56 rounded-full bg-gradient-to-br from-violet-600 via-fuchsia-600 to-indigo-700 flex flex-col items-center justify-center active:brightness-110"
          >
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ duration: 20, repeat: Infinity, ease: 'linear' }}
              className="absolute inset-3 rounded-full border border-dashed border-white/25"
            />
            <div className="absolute inset-6 rounded-full border border-white/10" />
            <div className="text-xs tracking-[0.3em] text-white/70 uppercase">Tap to</div>
            <div className="font-display text-3xl mt-1">Focus</div>
            <div className="text-sm text-white/70 mt-1">{activeSubject?.name ?? 'Pick a subject'}</div>
          </motion.button>
        </div>
      </div>

      {/* Subject chips row */}
      <div className="pb-4">
        <div className="px-5 text-[11px] tracking-widest uppercase text-white/40 mb-2">Your planets</div>
        <div className="flex gap-2 px-5 overflow-x-auto scrollbar-none">
          {subjects.map(s => {
            const sel = s.id === activeSubjectId;
            return (
              <button
                key={s.id}
                onClick={() => setActiveSubject(s.id)}
                className={`shrink-0 flex items-center gap-2 px-3 py-2 rounded-full border transition ${
                  sel ? 'bg-white/10 border-white/40' : 'bg-white/5 border-white/10 hover:border-white/25'
                }`}
                style={sel ? { boxShadow: `0 0 24px ${s.color}55` } : undefined}
              >
                <span className="w-2 h-2 rounded-full" style={{ background: s.color }} />
                <span className="text-xs">{s.name}</span>
              </button>
            );
          })}
          <button
            onClick={() => setScreen('subjectPicker')}
            className="shrink-0 px-3 py-2 rounded-full border border-white/10 bg-white/5 hover:border-white/25 text-xs text-white/70"
          >
            + New
          </button>
        </div>
      </div>

      {/* Nav bar */}
      <div className="flex items-center justify-around pb-4 pt-2 border-t border-white/5">
        <NavBtn label="Home" active onClick={() => setScreen('home')} />
        <NavBtn label="Galaxy" onClick={() => setScreen('galaxy')} />
        <NavBtn label="Notes" onClick={() => setScreen('notes')} />
        <NavBtn label="Circles" onClick={() => setScreen('circles')} />
        <NavBtn label="Stats" onClick={() => setScreen('stats')} />
      </div>

      <div className="px-5 pb-safe pb-3 text-center text-[11px] italic text-white/35">"{quote}"</div>
    </div>
  );
}

function NavBtn({ label, active, onClick }: { label: string; active?: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick} className={`px-3 py-2 text-xs tracking-wide ${active ? 'text-white' : 'text-white/50'}`}>
      {label}
    </button>
  );
}
