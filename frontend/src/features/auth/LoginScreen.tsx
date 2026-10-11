import { useState } from 'react';
import { motion } from 'framer-motion';
import { api, storeToken } from '../../data/api';
import { getOrCreateDeviceId } from '../../lib/uuid';
import { useStore } from '../../store';

export function LoginScreen() {
  const setScreen = useStore(s => s.setScreen);
  const setUser = useStore(s => s.setUser);
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setErr(null);
    setLoading(true);
    try {
      const clean = phone.replace(/\s|-/g, '');
      const { jwt, user } = await api.login(clean, password, getOrCreateDeviceId());
      storeToken(jwt);
      localStorage.setItem('sg.nameClaimed', '1');
      setUser(user);
      setScreen('home');
    } catch (e: any) {
      setErr('Wrong phone or password');
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-sm space-y-5"
      >
        <button onClick={() => setScreen('authLanding')} className="text-sm text-white/60">← Back</button>

        <h1 className="font-display text-4xl gradient-text">Welcome back</h1>
        <p className="text-sm text-white/60">Your galaxy is waiting.</p>

        <label className="block">
          <div className="text-xs tracking-wider uppercase text-white/50 mb-1.5">Phone number</div>
          <input
            type="tel"
            inputMode="tel"
            autoComplete="tel"
            placeholder="+91 98765 43210"
            value={phone}
            onChange={e => setPhone(e.target.value.replace(/[^0-9+\s-]/g, '').slice(0, 20))}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-violet-400/50 placeholder:text-white/30"
          />
        </label>

        <label className="block">
          <div className="text-xs tracking-wider uppercase text-white/50 mb-1.5">Password</div>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={e => setPassword(e.target.value.slice(0, 72))}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-violet-400/50"
          />
        </label>

        {err && (
          <motion.div
            animate={{ x: [0, -6, 6, -4, 4, 0] }}
            className="text-sm text-red-300 bg-red-500/10 border border-red-500/30 rounded-lg px-3 py-2"
          >
            {err}
          </motion.div>
        )}

        <motion.button
          whileTap={{ scale: 0.96 }}
          disabled={loading}
          onClick={submit}
          className="w-full py-4 rounded-2xl font-semibold text-lg bg-gradient-to-r from-violet-500 to-fuchsia-500 disabled:opacity-50"
        >
          {loading ? 'Opening your galaxy…' : 'Log in'}
        </motion.button>

        <div className="text-center text-xs text-white/50">
          No account yet?{' '}
          <button onClick={() => setScreen('signup')} className="underline underline-offset-2 text-violet-300">Create one</button>
        </div>
      </motion.div>
    </div>
  );
}
