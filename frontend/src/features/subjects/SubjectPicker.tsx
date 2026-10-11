import { useEffect, useState } from 'react';
import { useStore } from '../../store';
import { api, type Planet } from '../../data/api';

const PRESETS: [string, string][] = [
  ['Physics', '#60A5FA'],
  ['Chemistry', '#34D399'],
  ['Math', '#F472B6'],
  ['Biology', '#A78BFA'],
  ['English', '#FBBF24'],
  ['History', '#F97316'],
  ['Computer Science', '#22D3EE'],
  ['Economics', '#F43F5E'],
];

export function SubjectPicker() {
  const setScreen = useStore(s => s.setScreen);
  const setActiveSubject = useStore(s => s.setActiveSubject);
  const [subjects, setSubjects] = useState<Planet[]>([]);
  const [custom, setCustom] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { api.listSubjects().then(setSubjects).catch(() => {}); }, []);

  const pick = (id: string) => { setActiveSubject(id); setScreen('home'); };

  const create = async (name: string, color: string) => {
    if (busy || !name.trim()) return;
    setBusy(true);
    try {
      const created = await api.createSubject(name.trim(), color);
      setActiveSubject(created.id);
      setScreen('home');
    } catch (e) { console.error(e); setBusy(false); }
  };

  return (
    <div className="min-h-screen px-6 pt-6 pb-10 max-w-xl mx-auto">
      <button onClick={() => setScreen('home')} className="text-sm text-white/60">← Back</button>
      <h1 className="font-display text-4xl gradient-text mt-4">Pick a subject</h1>
      <p className="text-sm text-white/70 mt-2">Your focus session will grow this planet.</p>

      {subjects.length > 0 && (
        <section className="mt-6">
          <div className="text-xs tracking-widest uppercase text-white/50 mb-2">Your planets</div>
          <div className="space-y-2">
            {subjects.map(s => (
              <button
                key={s.id}
                onClick={() => pick(s.id)}
                className="w-full flex items-center justify-between rounded-xl bg-white/5 border border-white/10 hover:border-white/25 p-3 transition"
              >
                <div className="flex items-center gap-3">
                  <span className="w-3 h-3 rounded-full" style={{ background: s.color }} />
                  <span className="text-sm text-white/90">{s.name}</span>
                </div>
                <span className="text-xs text-white/50">{s.totalMin}m logged</span>
              </button>
            ))}
          </div>
        </section>
      )}

      <section className="mt-6">
        <div className="text-xs tracking-widest uppercase text-white/50 mb-2">Create a new planet</div>
        <div className="flex gap-2 mb-2">
          <input
            value={custom}
            onChange={e => setCustom(e.target.value.slice(0, 32))}
            placeholder="Custom (e.g. SAT prep)"
            className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 placeholder:text-white/30 focus:outline-none"
          />
          <button
            disabled={!custom.trim() || busy}
            onClick={() => create(custom, PRESETS[custom.length % PRESETS.length][1])}
            className="px-4 rounded-lg bg-violet-500 hover:bg-violet-400 disabled:opacity-40 text-sm font-medium"
          >Add</button>
        </div>
        <div className="text-xs text-white/50 mb-2">Or pick a preset:</div>
        <div className="flex flex-wrap gap-2">
          {PRESETS.map(([name, color]) => (
            <button
              key={name}
              disabled={busy}
              onClick={() => create(name, color)}
              className="flex items-center gap-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-full px-3 py-1.5 text-xs disabled:opacity-40"
            >
              <span className="w-2 h-2 rounded-full" style={{ background: color }} />
              {name}
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
