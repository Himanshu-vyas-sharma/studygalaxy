import { useEffect, useRef, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { COUNTRIES, type Country } from '../lib/countries';

interface Props {
  country: Country;
  onCountryChange: (c: Country) => void;
  value: string;              // 10 national digits only
  onValueChange: (v: string) => void;
  autoFocus?: boolean;
}

export function PhoneField({ country, onCountryChange, value, onValueChange, autoFocus }: Props) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const wrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, [open]);

  const filtered = query.trim()
    ? COUNTRIES.filter(c =>
        c.name.toLowerCase().includes(query.toLowerCase()) || c.dial.includes(query))
    : COUNTRIES;

  const filled = value.length === 10;

  return (
    <div ref={wrapRef} className="relative">
      <div
        className={`flex items-stretch rounded-xl overflow-hidden border transition-colors ${
          filled ? 'border-violet-500/70' : 'border-white/10'
        } bg-white/5 focus-within:border-violet-400`}
      >
        <button
          type="button"
          onClick={() => setOpen(o => !o)}
          className="flex items-center gap-1.5 px-3 shrink-0 hover:bg-white/5 transition-colors border-r border-white/10"
        >
          <span className="text-lg leading-none">{country.flag}</span>
          <span className="text-sm font-bold tabular-nums">{country.dial}</span>
          <motion.span
            animate={{ rotate: open ? 180 : 0 }}
            className="text-white/40 text-[10px] leading-none"
          >▼</motion.span>
        </button>

        <input
          type="tel"
          inputMode="numeric"
          autoComplete="tel-national"
          autoFocus={autoFocus}
          placeholder="10-digit number"
          value={value}
          onChange={e => onValueChange(e.target.value.replace(/\D/g, '').slice(0, 10))}
          className="flex-1 min-w-0 bg-transparent px-3 py-3.5 text-lg font-bold tracking-wide tabular-nums placeholder:font-normal placeholder:text-base placeholder:tracking-normal placeholder:text-white/30 focus:outline-none"
        />

        <div className="flex items-center pr-3 shrink-0">
          <span className={`text-[11px] font-bold tabular-nums ${filled ? 'text-violet-300' : 'text-white/30'}`}>
            {value.length}/10
          </span>
        </div>
      </div>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -8, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.98 }}
            transition={{ duration: 0.15 }}
            className="absolute z-50 mt-2 w-full rounded-xl border border-white/15 bg-[#120a2e] shadow-2xl shadow-black/60 overflow-hidden"
          >
            <input
              autoFocus
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder="Search country…"
              className="w-full bg-white/5 px-3 py-2.5 text-sm border-b border-white/10 focus:outline-none placeholder:text-white/30"
            />
            <div className="max-h-60 overflow-y-auto scrollbar-none">
              {filtered.map(c => (
                <button
                  key={c.code + c.dial}
                  type="button"
                  onClick={() => { onCountryChange(c); setOpen(false); setQuery(''); }}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 text-left hover:bg-white/10 transition-colors ${
                    c.code === country.code ? 'bg-violet-500/15' : ''
                  }`}
                >
                  <span className="text-lg">{c.flag}</span>
                  <span className="flex-1 text-sm">{c.name}</span>
                  <span className="text-xs font-bold text-white/60 tabular-nums">{c.dial}</span>
                </button>
              ))}
              {filtered.length === 0 && (
                <div className="px-3 py-4 text-sm text-white/40 text-center">No match</div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
