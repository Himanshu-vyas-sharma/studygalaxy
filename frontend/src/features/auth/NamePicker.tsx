import { useState } from 'react';
import { motion } from 'framer-motion';
import { api } from '../../data/api';
import { useStore } from '../../store';
import { Avatar } from '../../components/Avatar';

const SUGGESTIONS = [
  'NebulaNinja', 'QuasarQueen', 'AstroArjun', 'StellarRider',
  'CometHunter', 'NovaSpark', 'OrbitSage', 'LunarPilot',
];

export function NamePicker() {
  const user = useStore(s => s.user)!;
  const setUser = useStore(s => s.setUser);
  const setScreen = useStore(s => s.setScreen);
  const [name, setName] = useState(user.cosmicName);
  const [saving, setSaving] = useState(false);

  const save = async () => {
    const trimmed = name.trim();
    if (trimmed.length < 2) return;
    setSaving(true);
    try {
      await api.claimName(trimmed);
      setUser({ ...user, cosmicName: trimmed });
      setScreen('home');
    } catch (e) {
      console.error(e);
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="w-full max-w-sm text-center space-y-6"
      >
        <p className="text-xs tracking-[0.4em] text-violet-300/70 uppercase">Welcome to</p>
        <h1 className="font-display text-5xl gradient-text">StudyGalaxy</h1>
        <p className="text-white/70">Pick your cosmic name. This is who your friends will see when you focus.</p>

        <div className="flex justify-center py-2">
          <Avatar seed={user.avatarSeed} name={name || 'Star'} size={96} />
        </div>

        <input
          value={name}
          onChange={e => setName(e.target.value.replace(/[^A-Za-z0-9 ]/g, '').slice(0, 24))}
          placeholder="Your star name"
          className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-center text-lg focus:outline-none focus:ring-2 focus:ring-violet-400/50 placeholder:text-white/30"
          autoFocus
        />

        <div className="flex flex-wrap gap-2 justify-center">
          {SUGGESTIONS.map(s => (
            <button
              key={s}
              onClick={() => setName(s)}
              className="text-xs px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/10 border border-white/10 transition"
            >
              {s}
            </button>
          ))}
        </div>

        <button
          onClick={save}
          disabled={saving || name.trim().length < 2}
          className="w-full py-4 rounded-2xl font-semibold text-lg bg-gradient-to-r from-violet-500 to-fuchsia-500 disabled:opacity-40 transition active:scale-95"
        >
          {saving ? 'Charting course…' : 'Enter the galaxy'}
        </button>
      </motion.div>
    </div>
  );
}
