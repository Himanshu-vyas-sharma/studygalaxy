import { useEffect, useState } from 'react';
import { api } from '../../data/api';
import { useStore } from '../../store';

export function CirclesScreen() {
  const setScreen = useStore(s => s.setScreen);
  const [circles, setCircles] = useState<{ id: string; name: string; inviteCode: string }[]>([]);
  const [newName, setNewName] = useState('');
  const [joinCode, setJoinCode] = useState('');
  const [msg, setMsg] = useState<string | null>(null);

  const load = () => api.listCircles().then(setCircles).catch(console.error);
  useEffect(() => { load(); }, []);

  const create = async () => {
    if (newName.trim().length < 1) return;
    try {
      await api.createCircle(newName.trim());
      setNewName('');
      setMsg('Circle created');
      load();
    } catch (e) { console.error(e); }
  };

  const join = async () => {
    if (joinCode.trim().length < 4) return;
    try {
      await api.joinCircle(joinCode.trim().toUpperCase());
      setJoinCode('');
      setMsg('Joined circle');
      load();
    } catch (e) {
      setMsg('Invalid code');
    }
  };

  return (
    <div className="min-h-screen px-5 pt-4 pb-10">
      <div className="flex items-center justify-between">
        <button onClick={() => setScreen('home')} className="text-sm text-white/70">← Back</button>
        <div className="font-display text-xl">Study Circles</div>
        <div className="w-8" />
      </div>

      <div className="mt-8 space-y-3">
        <div className="rounded-2xl bg-white/5 border border-white/10 p-4">
          <div className="text-sm font-semibold mb-2">Join a circle</div>
          <div className="flex gap-2">
            <input
              value={joinCode}
              onChange={e => setJoinCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6))}
              placeholder="6-char code"
              className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 tracking-widest uppercase placeholder:text-white/30 focus:outline-none"
            />
            <button onClick={join} className="px-4 rounded-lg bg-gradient-to-r from-violet-500 to-fuchsia-500 font-medium">Join</button>
          </div>
        </div>

        <div className="rounded-2xl bg-white/5 border border-white/10 p-4">
          <div className="text-sm font-semibold mb-2">Create a circle</div>
          <div className="flex gap-2">
            <input
              value={newName}
              onChange={e => setNewName(e.target.value.slice(0, 32))}
              placeholder="e.g. IIT Prep Squad"
              className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 placeholder:text-white/30 focus:outline-none"
            />
            <button onClick={create} className="px-4 rounded-lg bg-white/10 font-medium">Create</button>
          </div>
        </div>

        {msg && <div className="text-xs text-white/60 text-center">{msg}</div>}
      </div>

      <div className="mt-8">
        <div className="text-xs tracking-widest uppercase text-white/40 mb-2">Your circles</div>
        {circles.length === 0 && <div className="text-sm text-white/50 mt-2">You haven't joined any circles yet.</div>}
        <div className="space-y-2 mt-2">
          {circles.map(c => (
            <div key={c.id} className="rounded-xl bg-white/5 border border-white/10 p-3 flex items-center justify-between">
              <div>
                <div className="font-semibold">{c.name}</div>
                <div className="text-xs text-white/50 mt-0.5">Code: <span className="tracking-widest">{c.inviteCode}</span></div>
              </div>
              <button
                onClick={() => navigator.clipboard?.writeText(c.inviteCode)}
                className="text-xs px-3 py-1.5 rounded-full bg-white/10"
              >
                Copy
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
