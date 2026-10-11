import { useEffect, useRef } from 'react';

interface StarDot {
  x: number;
  y: number;
  r: number;
  twinkle: number;
  speed: number;
  hue: number;
}

export function GalaxyBackground() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const starsRef = useRef<StarDot[]>([]);
  const shootingRef = useRef<{ x: number; y: number; vx: number; vy: number; life: number } | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current!;
    const ctx = canvas.getContext('2d')!;
    let raf = 0;

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = window.innerWidth * dpr;
      canvas.height = window.innerHeight * dpr;
      canvas.style.width = `${window.innerWidth}px`;
      canvas.style.height = `${window.innerHeight}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const count = Math.round((window.innerWidth * window.innerHeight) / 4500);
      starsRef.current = Array.from({ length: count }, () => ({
        x: Math.random() * window.innerWidth,
        y: Math.random() * window.innerHeight,
        r: Math.random() * 1.4 + 0.3,
        twinkle: Math.random() * Math.PI * 2,
        speed: 0.4 + Math.random() * 1.5,
        hue: 220 + Math.random() * 80,
      }));
    };
    resize();
    window.addEventListener('resize', resize);

    const start = performance.now();
    const render = (t: number) => {
      const elapsed = (t - start) / 1000;
      const w = window.innerWidth;
      const h = window.innerHeight;

      // Deep space base
      const grad = ctx.createRadialGradient(w * 0.5, h * 0.4, 0, w * 0.5, h * 0.4, Math.max(w, h));
      grad.addColorStop(0, '#140833');
      grad.addColorStop(0.5, '#0A0520');
      grad.addColorStop(1, '#05020D');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, w, h);

      // Soft nebula clouds
      const addNebula = (x: number, y: number, r: number, color: string) => {
        const g = ctx.createRadialGradient(x, y, 0, x, y, r);
        g.addColorStop(0, color);
        g.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = g;
        ctx.fillRect(0, 0, w, h);
      };
      ctx.globalCompositeOperation = 'lighter';
      addNebula(w * 0.3 + Math.sin(elapsed * 0.1) * 20, h * 0.3, Math.max(w, h) * 0.4, 'rgba(139,92,246,0.18)');
      addNebula(w * 0.75 + Math.cos(elapsed * 0.08) * 20, h * 0.7, Math.max(w, h) * 0.45, 'rgba(236,72,153,0.14)');
      addNebula(w * 0.6, h * 0.2, Math.max(w, h) * 0.3, 'rgba(59,130,246,0.1)');
      ctx.globalCompositeOperation = 'source-over';

      // Stars
      for (const s of starsRef.current) {
        const alpha = 0.4 + 0.6 * (0.5 + 0.5 * Math.sin(elapsed * s.speed + s.twinkle));
        ctx.beginPath();
        ctx.fillStyle = `hsla(${s.hue} 90% 85% / ${alpha})`;
        ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
        ctx.fill();
      }

      // Shooting star
      if (!shootingRef.current && Math.random() < 0.004) {
        shootingRef.current = {
          x: Math.random() * w,
          y: Math.random() * h * 0.4,
          vx: 6 + Math.random() * 4,
          vy: 2 + Math.random() * 2,
          life: 1,
        };
      }
      const sh = shootingRef.current;
      if (sh) {
        ctx.strokeStyle = `rgba(255,255,255,${sh.life})`;
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(sh.x, sh.y);
        ctx.lineTo(sh.x - sh.vx * 8, sh.y - sh.vy * 8);
        ctx.stroke();
        sh.x += sh.vx;
        sh.y += sh.vy;
        sh.life -= 0.015;
        if (sh.life <= 0) shootingRef.current = null;
      }

      raf = requestAnimationFrame(render);
    };
    raf = requestAnimationFrame(render);

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', resize);
    };
  }, []);

  return <canvas ref={canvasRef} className="fixed inset-0 -z-10" />;
}
