import { useState } from 'react';
import { motion } from 'framer-motion';
import { api, storeToken } from '../../data/api';
import { getOrCreateDeviceId } from '../../lib/uuid';
import { guessCountry, type Country } from '../../lib/countries';
import { PhoneField } from '../../components/PhoneField';
import { useStore } from '../../store';

export function SignupScreen() {
  const setScreen = useStore(s => s.setScreen);
  const setUser = useStore(s => s.setUser);
  const [country, setCountry] = useState<Country>(() => guessCountry());
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setErr(null);
    if (phone.length !== 10) return setErr('Enter exactly 10 digits');
    if (password.length < 8) return setErr('Password must be at least 8 characters');
    if (password !== confirm) return setErr('Passwords do not match');

    setLoading(true);
    try {
      const { jwt, user } = await api.signup(country.dial, phone, password, getOrCreateDeviceId());
      storeToken(jwt);
      setUser(user);
      setScreen('namePicker');
    } catch (e: any) {
      const msg = String(e?.message ?? '');
      if (msg.includes('phone_taken')) setErr('This number already has an account — log in instead');
      else if (msg.includes('bad_phone')) setErr('That number does not look right');
      else if (msg.includes('too_many_requests')) setErr('Too many attempts. Wait a few minutes.');
      else if (msg.includes('Failed to fetch')) setErr('Cannot reach the server. Check your connection.');
      else setErr('Sign up failed. Try again.');
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-6 py-10">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
        className="w-full max-w-sm space-y-5"
      >
        <button onClick={() => setScreen('authLanding')} className="text-sm font-semibold text-white/60">← Back</button>

        <div>
          <motion.h1
            initial={{ opacity: 0, letterSpacing: '0.2em' }}
            animate={{ opacity: 1, letterSpacing: '-0.02em' }}
            transition={{ duration: 0.7, ease: 'easeOut' }}
            className="font-display text-5xl gradient-text"
          >
            Create account
          </motion.h1>
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="text-sm font-medium text-white/70 mt-2"
          >
            Free for students everywhere. We never send SMS — your number is just your login.
          </motion.p>
        </div>

        <Field label="Phone number">
          <PhoneField
            country={country}
            onCountryChange={setCountry}
            value={phone}
            onValueChange={setPhone}
            autoFocus
          />
        </Field>

        <Field label="Password">
          <input
            type="password"
            autoComplete="new-password"
            placeholder="At least 8 characters"
            value={password}
            onChange={e => setPassword(e.target.value.slice(0, 72))}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3.5 text-lg font-bold focus:outline-none focus:border-violet-400 placeholder:font-normal placeholder:text-base placeholder:text-white/30"
          />
        </Field>

        <Field label="Confirm password">
          <input
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={e => setConfirm(e.target.value.slice(0, 72))}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3.5 text-lg font-bold focus:outline-none focus:border-violet-400"
          />
        </Field>

        {err && (
          <motion.div
            key={err}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1, x: [0, -7, 7, -5, 5, 0] }}
            transition={{ duration: 0.4 }}
            className="text-sm font-semibold text-red-300 bg-red-500/10 border border-red-500/30 rounded-xl px-4 py-3"
          >
            {err}
          </motion.div>
        )}

        <motion.button
          whileTap={{ scale: 0.97 }}
          whileHover={{ scale: 1.01 }}
          disabled={loading}
          onClick={submit}
          className="w-full py-4 rounded-2xl font-extrabold text-lg tracking-tight bg-gradient-to-r from-violet-500 to-fuchsia-500 shadow-lg shadow-violet-500/30 disabled:opacity-50"
        >
          {loading ? 'Charting your galaxy…' : 'Create account'}
        </motion.button>

        <div className="text-center text-xs font-medium text-white/50">
          Already have an account?{' '}
          <button onClick={() => setScreen('login')} className="font-bold underline underline-offset-2 text-violet-300">
            Log in
          </button>
        </div>
      </motion.div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <div className="text-[11px] font-bold tracking-[0.18em] uppercase text-white/50 mb-2">{label}</div>
      {children}
    </label>
  );
}
