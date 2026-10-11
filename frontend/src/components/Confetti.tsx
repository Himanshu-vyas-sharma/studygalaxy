import { useEffect, useRef } from 'react';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  rot: number;
  vr: number;
  size: number;
  color: string;
  life: number;
}

const COLORS = ['#C4B5FD', '#F472B6', '#FBBF24', '#60A5FA', '#F87171', '#34D399'];

export function Confetti({ fire }: { fire: boolean }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const partsRef = useRef<Particle[]>([]);

  useEffect(() => {
    if (!fire) return;
    const canvas = canvasRef.current!;
    const ctx = canvas.getContext('2d')!;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const w = window.innerWidth;
    const h = window.innerHeight;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    canvas.style.width = `${w}px`;
    canvas.style.height = `${h}px`;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    partsRef.current = Array.from({ length: 140 }, () => ({
      x: w / 2,
      y: h / 2,
      vx: (Math.random() - 0.5) * 14,
      vy: (Math.random() - 0.9) * 14,
      rot: Math.random() * Math.PI,
      vr: (Math.random() - 0.5) * 0.3,
      size: 4 + Math.random() * 6,
      color: COLORS[Math.floor(Math.random() * COLORS.length)],
      life: 1,
    }));

    let raf = 0;
    const render = () => {
      ctx.clearRect(0, 0, w, h);
      let alive = 0;
      for (const p of partsRef.current) {
        if (p.life <= 0) continue;
        alive++;
        p.vy += 0.35;
        p.vx *= 0.995;
        p.x += p.vx;
        p.y += p.vy;
        p.rot += p.vr;
        p.life -= 0.012;
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rot);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = Math.max(0, p.life);
        ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.4);
        ctx.restore();
      }
      if (alive > 0) raf = requestAnimationFrame(render);
    };
    raf = requestAnimationFrame(render);
    return () => cancelAnimationFrame(raf);
  }, [fire]);

  if (!fire) return null;
  return <canvas ref={canvasRef} className="fixed inset-0 pointer-events-none z-50" />;
}
