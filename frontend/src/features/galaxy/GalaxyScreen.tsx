import { useEffect, useRef, useState } from 'react';
import { api, type Planet, type Star, type BlackHole } from '../../data/api';
import { useStore } from '../../store';
import { stageFor } from '../../lib/planetStage';

interface GalaxyData {
  planets: Planet[];
  stars: Star[];
  blackHoles: BlackHole[];
}

export function GalaxyScreen() {
  const setScreen = useStore(s => s.setScreen);
  const [data, setData] = useState<GalaxyData | null>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    api.galaxy().then(setData).catch(console.error);
  }, []);

  useEffect(() => {
    if (!data || !canvasRef.current) return;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d')!;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const resize = () => {
      canvas.width = window.innerWidth * dpr;
      canvas.height = (window.innerHeight - 120) * dpr;
      canvas.style.width = `${window.innerWidth}px`;
      canvas.style.height = `${window.innerHeight - 120}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();

    let raf = 0;
    const start = performance.now();
    const render = (t: number) => {
      const elapsed = (t - start) / 1000;
      const w = window.innerWidth;
      const h = window.innerHeight - 120;
      ctx.clearRect(0, 0, w, h);

      // Planets arranged on a circle, sized by total minutes
      const cx = w / 2;
      const cy = h / 2;
      const orbitR = Math.min(w, h) * 0.32;
      const planetCount = data.planets.length;
      data.planets.forEach((p, i) => {
        const angle = (i / Math.max(planetCount, 1)) * Math.PI * 2 + elapsed * 0.05;
        const px = cx + Math.cos(angle) * orbitR;
        const py = cy + Math.sin(angle) * orbitR;
        const r = 14 + Math.min(p.totalMin / 20, 32);
        const stage = stageFor(p.totalMin);

        // Rings sit behind the planet body.
        if (stage.rings) {
          ctx.save();
          ctx.translate(px, py);
          ctx.scale(1, 0.3);
          ctx.strokeStyle = p.color;
          ctx.globalAlpha = 0.45;
          ctx.lineWidth = 2.5;
          ctx.beginPath();
          ctx.arc(0, 0, r * 2.1, 0, Math.PI * 2);
          ctx.stroke();
          ctx.globalAlpha = 0.18;
          ctx.strokeStyle = '#ffffff';
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          ctx.arc(0, 0, r * 1.72, 0, Math.PI * 2);
          ctx.stroke();
          ctx.restore();
          ctx.globalAlpha = 1;
        }

        const g = ctx.createRadialGradient(px, py, 0, px, py, r * 1.8);
        g.addColorStop(0, p.color);
        g.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(px, py, r * 1.8, 0, Math.PI * 2);
        ctx.fill();

        // Atmosphere halo.
        if (stage.atmosphere) {
          const a = ctx.createRadialGradient(px, py, r * 0.9, px, py, r * 1.45);
          a.addColorStop(0, 'rgba(0,0,0,0)');
          a.addColorStop(1, p.color);
          ctx.globalAlpha = 0.5;
          ctx.fillStyle = a;
          ctx.beginPath();
          ctx.arc(px, py, r * 1.45, 0, Math.PI * 2);
          ctx.fill();
          ctx.globalAlpha = 1;
        }

        ctx.fillStyle = p.color;
        ctx.beginPath();
        ctx.arc(px, py, r, 0, Math.PI * 2);
        ctx.fill();

        // Oceans.
        if (stage.water) {
          ctx.fillStyle = 'rgba(43,108,176,0.55)';
          ctx.beginPath();
          ctx.arc(px - r * 0.33, py - r * 0.22, r * 0.42, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = 'rgba(43,108,176,0.40)';
          ctx.beginPath();
          ctx.arc(px + r * 0.36, py + r * 0.3, r * 0.3, 0, Math.PI * 2);
          ctx.fill();
        }

        // Life: continents that pulse.
        if (stage.life) {
          const pulse = 0.75 + 0.25 * Math.sin(elapsed * 1.4 + i);
          ctx.fillStyle = `rgba(52,211,153,${0.6 * pulse})`;
          ctx.beginPath();
          ctx.arc(px + r * 0.1, py - r * 0.38, r * 0.26, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = `rgba(52,211,153,${0.45 * pulse})`;
          ctx.beginPath();
          ctx.arc(px - r * 0.4, py + r * 0.34, r * 0.2, 0, Math.PI * 2);
          ctx.fill();
        }

        // Moons.
        for (let m = 0; m < stage.moons; m++) {
          const ma = elapsed * (0.6 + m * 0.25) + m * 2.3;
          const md = r * (1.9 + m * 0.45);
          ctx.fillStyle = '#cbd5e1';
          ctx.beginPath();
          ctx.arc(px + Math.cos(ma) * md, py + Math.sin(ma) * md * 0.55, 2.6 + m * 0.4, 0, Math.PI * 2);
          ctx.fill();
        }

        ctx.fillStyle = 'rgba(255,255,255,0.9)';
        ctx.font = '12px Inter, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(p.name, px, py + r + 16);
        ctx.fillStyle = 'rgba(255,255,255,0.45)';
        ctx.font = '10px Inter, sans-serif';
        ctx.fillText(stage.label, px, py + r + 30);
      });

      // Stars (completed sessions) scattered around their planets
      data.stars.forEach((s, idx) => {
        const planetIdx = data.planets.findIndex(p => p.id === s.subjectId);
        const planetAngle =
          planetIdx >= 0
            ? (planetIdx / Math.max(data.planets.length, 1)) * Math.PI * 2 + elapsed * 0.05
            : (idx / 10) * Math.PI * 2;
        const px = cx + Math.cos(planetAngle) * orbitR;
        const py = cy + Math.sin(planetAngle) * orbitR;
        const offsetAngle = (idx * 1.37) % (Math.PI * 2);
        const offsetDist = 40 + ((idx * 17) % 60);
        const sx = px + Math.cos(offsetAngle) * offsetDist;
        const sy = py + Math.sin(offsetAngle) * offsetDist;
        const alpha = 0.4 + (s.rating / 5) * 0.6;
        const twinkle = 0.7 + 0.3 * Math.sin(elapsed * 2 + idx);
        ctx.fillStyle = `rgba(255,245,200,${alpha * twinkle})`;
        ctx.beginPath();
        ctx.arc(sx, sy, 2.5, 0, Math.PI * 2);
        ctx.fill();
      });

      // Black holes — permanent scars
      data.blackHoles.forEach((b, idx) => {
        const planetIdx = data.planets.findIndex(p => p.id === b.subjectId);
        const planetAngle =
          planetIdx >= 0
            ? (planetIdx / Math.max(data.planets.length, 1)) * Math.PI * 2 + elapsed * 0.05
            : 0;
        const px = cx + Math.cos(planetAngle) * orbitR;
        const py = cy + Math.sin(planetAngle) * orbitR;
        const offsetAngle = (idx * 2.1) % (Math.PI * 2);
        const bx = px + Math.cos(offsetAngle) * 70;
        const by = py + Math.sin(offsetAngle) * 70;
        ctx.fillStyle = '#000';
        ctx.beginPath();
        ctx.arc(bx, by, 5, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = 'rgba(139,92,246,0.5)';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(bx, by, 7, 0, Math.PI * 2);
        ctx.stroke();
      });

      raf = requestAnimationFrame(render);
    };
    raf = requestAnimationFrame(render);

    return () => cancelAnimationFrame(raf);
  }, [data]);

  return (
    <div className="min-h-screen flex flex-col">
      <div className="px-5 pt-4 flex items-center justify-between">
        <button onClick={() => setScreen('home')} className="text-sm text-white/70">← Back</button>
        <div className="font-display text-xl">Your Galaxy</div>
        <div className="w-8" />
      </div>
      <div className="text-center text-xs text-white/40 mt-1">
        {data ? `${data.planets.length} planets · ${data.stars.length} stars · ${data.blackHoles.length} black holes` : 'Loading…'}
      </div>
      <canvas ref={canvasRef} className="flex-1" />
    </div>
  );
}
