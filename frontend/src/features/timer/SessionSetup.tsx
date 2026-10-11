import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { useStore } from '../../store';
import { api, type Planet } from '../../data/api';

const DURATIONS = [25, 45, 90];

export function SessionSetup() {
  const setScreen = useStore(s => s.setScreen);
  const activeSubjectId = useStore(s => s.activeSubjectId);
  const setActiveSession = useStore(s => s.setActiveSession);
  const setActiveTodos = useStore(s => s.setActiveTodos);
  const [subjects, setSubjects] = useState<Planet[]>([]);
  const [chosenMin, setChosenMin] = useState(25);
  const [intention, setIntention] = useState('');
  const [todos, setTodos] = useState<string[]>(['', '', '']);
  const [starting, setStarting] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => { api.listSubjects().then(setSubjects).catch(() => {}); }, []);
  const activeSubject = subjects.find(s => s.id === activeSubjectId);

  const readyTodos = todos.map(t => t.trim()).filter(Boolean);
  const canStart = !!activeSubject && readyTodos.length >= 1;

  const addRow = () => todos.length < 5 && setTodos([...todos, '']);
  const removeRow = (i: number) => setTodos(todos.filter((_, j) => j !== i));

  const start = async () => {
    if (!canStart) { setErr('Pick a subject and at least one to-do'); return; }
    setStarting(true); setErr(null);
    try {
      const { sessionId, serverStartTs, todoIds } = await api.startSessionWithTodos(
        activeSubjectId!, chosenMin, intention, readyTodos
      );
      setActiveSession(sessionId, serverStartTs + chosenMin * 60_000, activeSubjectId);
      setActiveTodos(todoIds.map((id, i) => ({ id, text: readyTodos[i], done: false })));
      setScreen('timer');
    } catch (e) {
      console.error(e);
      setErr('Could not start — try again.');
      setStarting(false);
    }
  };

  return (
    <div className="min-h-screen px-6 pt-6 pb-16 max-w-xl mx-auto">
      <button onClick={() => setScreen('home')} className="text-sm text-white/60">← Back</button>

      <motion.h1
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        className="font-display text-4xl gradient-text mt-4"
      >
        Ready to focus?
      </motion.h1>
      <p className="text-sm text-white/70 mt-2">Set your subject, duration, and what you'll crush.</p>

      {/* Subject */}
      <section className="mt-6">
        <div className="text-xs tracking-widest uppercase text-white/50 mb-2">Subject</div>
        <button
          onClick={() => setScreen('subjectPicker')}
          className={`w-full flex items-center justify-between rounded-xl p-3 border bg-white/5 ${
            activeSubject ? 'border-violet-500/60' : 'border-red-400/40'
          }`}
        >
          <div className="flex items-center gap-3">
            {activeSubject ? (
              <>
                <span className="w-3 h-3 rounded-full" style={{ background: activeSubject.color }} />
                <span className="text-sm text-white/90">{activeSubject.name}</span>
              </>
            ) : (
              <span className="text-sm text-red-300">No subject chosen — tap to pick</span>
            )}
          </div>
          <span className="text-white/50">›</span>
        </button>
      </section>

      {/* Duration */}
      <section className="mt-5">
        <div className="text-xs tracking-widest uppercase text-white/50 mb-2">Duration</div>
        <div className="grid grid-cols-3 gap-2">
          {DURATIONS.map(m => (
            <button
              key={m}
              onClick={() => setChosenMin(m)}
              className={`py-3 rounded-xl text-base font-medium border transition ${
                m === chosenMin
                  ? 'bg-gradient-to-r from-violet-500/40 to-fuchsia-500/30 border-violet-400'
                  : 'bg-white/5 border-white/10 hover:border-white/25'
              }`}
            >
              {m}m
            </button>
          ))}
        </div>
      </section>

      {/* Intention */}
      <section className="mt-5">
        <div className="text-xs tracking-widest uppercase text-white/50 mb-2">Intention (optional)</div>
        <input
          value={intention}
          onChange={e => setIntention(e.target.value.slice(0, 140))}
          placeholder="Finish ch. 4 derivations"
          className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 placeholder:text-white/30 focus:outline-none focus:ring-2 focus:ring-violet-400/40"
        />
      </section>

      {/* Todos */}
      <section className="mt-5">
        <div className="flex items-center justify-between mb-2">
          <div className="text-xs tracking-widest uppercase text-white/50">What will you crush?</div>
          {todos.length < 5 && (
            <button onClick={addRow} className="text-xs text-violet-300 hover:text-violet-200">+ add</button>
          )}
        </div>
        <div className="space-y-2">
          {todos.map((t, i) => (
            <div key={i} className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-lg px-3 py-2">
              <span className="w-4 h-4 rounded-full border border-white/30" />
              <input
                value={t}
                onChange={e => setTodos(todos.map((v, j) => (j === i ? e.target.value.slice(0, 100) : v)))}
                placeholder={`todo ${i + 1}`}
                className="flex-1 bg-transparent focus:outline-none text-sm placeholder:text-white/30"
              />
              {todos.length > 1 && (
                <button onClick={() => removeRow(i)} className="text-white/40 hover:text-white/70">×</button>
              )}
            </div>
          ))}
        </div>
      </section>

      {err && (
        <motion.div
          animate={{ x: [0, -6, 6, -4, 4, 0] }}
          className="mt-4 text-sm text-red-300 bg-red-500/10 border border-red-500/30 rounded-lg px-3 py-2"
        >{err}</motion.div>
      )}

      <motion.button
        whileTap={{ scale: 0.96 }}
        disabled={starting || !canStart}
        onClick={start}
        className="mt-6 w-full py-4 rounded-2xl font-semibold text-lg bg-gradient-to-r from-violet-500 to-fuchsia-500 disabled:opacity-40"
      >
        {starting ? 'Opening the portal…' : canStart ? 'Enter the portal' : 'Pick a subject + a to-do'}
      </motion.button>
    </div>
  );
}
