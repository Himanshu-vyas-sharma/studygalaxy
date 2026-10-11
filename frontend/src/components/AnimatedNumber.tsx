import { useEffect, useRef, useState } from 'react';

interface Props {
  value: number;
  durationMs?: number;
  format?: (n: number) => string;
}

export function AnimatedNumber({ value, durationMs = 900, format }: Props) {
  const [display, setDisplay] = useState(value);
  const fromRef = useRef(value);
  const startRef = useRef(0);
  const rafRef = useRef(0);

  useEffect(() => {
    fromRef.current = display;
    startRef.current = performance.now();
    const target = value;
    const from = display;
    const tick = (t: number) => {
      const p = Math.min(1, (t - startRef.current) / durationMs);
      // easeOutCubic
      const eased = 1 - Math.pow(1 - p, 3);
      const curr = Math.round(from + (target - from) * eased);
      setDisplay(curr);
      if (p < 1) rafRef.current = requestAnimationFrame(tick);
    };
    rafRef.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  return <>{format ? format(display) : display}</>;
}
