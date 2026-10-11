import { useEffect, useState } from 'react';
import { api } from '../../data/api';
import { useStore } from '../../store';

export function StatsScreen() {
  const setScreen = useStore(s => s.setScreen);
  const [stats, setStats] = useState<{
    heatmap: { date: string; min: number }[];
    weekTotalMin: number;
    streakDays: number;
    totalFocusMin: number;
    level: number;
  } | null>(null);

  useEffect(() => { api.statsWeekly().then(setStats).catch(console.error); }, []);

  const peak = stats ? Math.max(1, ...stats.heatmap.map(d => d.min)) : 1;

  return (
    <div className="min-h-screen px-5 pt-4 pb-10">
      <div className="flex items-center justify-between">
        <button onClick={() => setScreen('home')} className="text-sm text-white/70">← Back</button>
        <div className="font-display text-xl">Weekly Stats</div>
        <div className="w-8" />
      </div>

      {stats && (
        <div className="mt-8 space-y-6">
          <div className="grid grid-cols-3 gap-3 text-center">
            <StatTile label="Week" value={`${stats.weekTotalMin}m`} />
            <StatTile label="Streak" value={`${stats.streakDays}d`} />
            <StatTile label="Level" value={`${stats.level}`} />
          </div>

          <div>
            <div className="text-xs tracking-widest uppercase text-white/40 mb-2">Last 7 days</div>
            <div className="flex items-end gap-2 h-32">
              {stats.heatmap.map(d => (
                <div key={d.date} className="flex-1 flex flex-col items-center gap-1">
                  <div
                    className="w-full rounded-md bg-gradient-to-t from-violet-700 to-fuchsia-400"
                    style={{ height: `${(d.min / peak) * 100}%`, minHeight: 4 }}
                    title={`${d.date}: ${d.min} min`}
                  />
                  <div className="text-[10px] text-white/50">{d.date.slice(5)}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-2xl p-5 bg-gradient-to-br from-violet-600/20 to-fuchsia-600/20 border border-white/10 text-center">
            <div className="font-display text-3xl gradient-text">{Math.floor(stats.totalFocusMin / 60)}h total</div>
            <div className="text-xs text-white/60 mt-1">All focus you've ever logged in this galaxy.</div>
          </div>
        </div>
      )}
    </div>
  );
}

function StatTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-white/5 border border-white/10 p-3">
      <div className="text-[10px] tracking-widest uppercase text-white/40">{label}</div>
      <div className="font-display text-2xl gradient-text mt-1">{value}</div>
    </div>
  );
}
