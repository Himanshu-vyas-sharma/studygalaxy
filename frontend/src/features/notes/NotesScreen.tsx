import { useEffect, useState } from 'react';
import { useStore } from '../../store';
import { api, type Note } from '../../data/api';

export function NotesScreen() {
  const setScreen = useStore(s => s.setScreen);
  const setEditingNote = useStore(s => s.setEditingNote);
  const [notes, setNotes] = useState<Note[]>([]);

  useEffect(() => { api.listNotes().then(setNotes).catch(() => {}); }, []);

  const openNew = () => { setEditingNote(null); setScreen('noteCanvas'); };
  const openExisting = (n: Note) => { setEditingNote(n.id); setScreen('noteCanvas'); };

  return (
    <div className="min-h-screen px-5 pt-6 pb-24 max-w-xl mx-auto relative">
      <div className="flex items-center justify-between">
        <button onClick={() => setScreen('home')} className="text-sm text-white/70">← Back</button>
        <div className="font-display text-xl">Notes</div>
        <div className="w-8" />
      </div>

      {notes.length === 0 ? (
        <div className="mt-24 text-center">
          <div className="text-white/70">No notes yet.</div>
          <div className="text-xs text-white/50 mt-1">Tap + to sketch your first one.</div>
        </div>
      ) : (
        <div className="mt-6 grid grid-cols-2 gap-3">
          {notes.map(n => (
            <button
              key={n.id}
              onClick={() => openExisting(n)}
              className="rounded-2xl bg-white/5 border border-white/10 hover:border-white/25 p-3 text-left transition"
            >
              <div className="h-28 rounded-lg bg-gradient-to-br from-indigo-900/40 to-black overflow-hidden relative">
                <ThumbStrokes strokes={n.strokes} />
              </div>
              <div className="text-sm mt-2 truncate">{n.title.trim() || 'Untitled note'}</div>
            </button>
          ))}
        </div>
      )}

      <button
        onClick={openNew}
        className="fixed bottom-6 right-6 w-14 h-14 rounded-full bg-violet-500 hover:bg-violet-400 text-2xl font-bold shadow-lg shadow-violet-500/40"
      >
        +
      </button>
    </div>
  );
}

function ThumbStrokes({ strokes }: { strokes: Note['strokes'] }) {
  if (!strokes?.length) return null;
  const all = strokes.flatMap(s => s.points);
  if (!all.length) return null;
  const xs = all.map(p => p[0]); const ys = all.map(p => p[1]);
  const minX = Math.min(...xs); const maxX = Math.max(...xs);
  const minY = Math.min(...ys); const maxY = Math.max(...ys);
  const dx = Math.max(1, maxX - minX); const dy = Math.max(1, maxY - minY);
  const pad = 4;
  const w = 200; const h = 112;
  const sx = (p: number) => pad + ((p - minX) / dx) * (w - pad * 2);
  const sy = (p: number) => pad + ((p - minY) / dy) * (h - pad * 2);
  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="absolute inset-0 w-full h-full">
      {strokes.map((s, i) => (
        <polyline
          key={i}
          fill="none"
          stroke={s.erase ? '#000' : s.color}
          strokeWidth={Math.max(1, s.width * 0.35)}
          strokeLinecap="round"
          strokeLinejoin="round"
          points={s.points.map(p => `${sx(p[0])},${sy(p[1])}`).join(' ')}
        />
      ))}
    </svg>
  );
}
