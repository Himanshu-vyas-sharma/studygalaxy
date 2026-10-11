import { useState } from 'react';
import { motion } from 'framer-motion';
import { api, storeToken } from '../../data/api';
import { getOrCreateDeviceId } from '../../lib/uuid';
import { useStore } from '../../store';

const PHONE_RE = /^\+?[0-9]{10,15}$/;

export function SignupScreen() {
  const setScreen = useStore(s => s.setScreen);
  const setUser = useStore(s => s.setUser);
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    setErr(null);
    const clean = phone.replace(/\s|-/g, '');
    if (!PHONE_RE.test(clean)) return setErr('Enter a valid phone number (10-15 digits)');
    if (password.length < 6) return setErr('Password must be at least 6 characters');
    if (password !== confirm) return setErr('Passwords do not match');

    setLoading(true);
    try {
      const { jwt, user } = await api.signup(clean, password, getOrCreateDeviceId());
      storeToken(jwt);
      setUser(user);
      setScreen('namePicker');
    } catch (e: any) {
      const msg = String(e?.message ?? '');
      if (msg.includes('phone_taken')) setErr('This phone number already has an account');
      else if (msg.includes('bad_phone')) setErr('Enter a valid phone number');
      else setErr('Sign up failed. Try again.');
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-sm space-y-5"
      >
        <button onClick={() => setScreen('authLanding')} className="text-sm text-white/60">← Back</button>

        <h1 className="font-display text-4xl gradient-text">Create account</h1>
        <p className="text-sm text-white/60">Your phone is your key. We never send SMS — we never spend your money.</p>

        <Field label="Phone number">
          <input
            type="tel"
            inputMode="tel"
            autoComplete="tel"
            placeholder="+91 98765 43210"
            value={phone}
            onChange={e => setPhone(e.target.value.replace(/[^0-9+\s-]/g, '').slice(0, 20))}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-violet-400/50 placeholder:text-white/30"
          />
        </Field>

        <Field label="Password">
          <input
            type="password"
            autoComplete="new-password"
            placeholder="At least 6 characters"
            value={password}
            onChange={e => setPassword(e.target.value.slice(0, 72))}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-violet-400/50 placeholder:text-white/30"
          />
        </Field>

        <Field label="Confirm password">
          <input
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={e => setConfirm(e.target.value.slice(0, 72))}
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-violet-400/50"
          />
        </Field>

        {err && (
          <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: [0, -6, 6, -4, 4, 0] }}
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
          {loading ? 'Charting your galaxy…' : 'Create account'}
        </motion.button>

        <div className="text-center text-xs text-white/50">
          Already have an account?{' '}
          <button onClick={() => setScreen('login')} className="underline underline-offset-2 text-violet-300">Log in</button>
        </div>
      </motion.div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <div className="text-xs tracking-wider uppercase text-white/50 mb-1.5">{label}</div>
      {children}
    </label>
  );
}
