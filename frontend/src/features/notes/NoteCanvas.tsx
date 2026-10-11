import { useEffect, useRef, useState } from 'react';
import { useStore } from '../../store';
import { api, type Note, type Stroke } from '../../data/api';

const PALETTE = ['#FFFFFF', '#FBBF24', '#F472B6', '#A78BFA', '#60A5FA', '#34D399', '#F97316', '#F43F5E'];
const WIDTHS = [3, 7, 14];

export function NoteCanvas() {
  const setScreen = useStore(s => s.setScreen);
  const editingNoteId = useStore(s => s.editingNoteId);
  const activeSubjectId = useStore(s => s.activeSubjectId);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [strokes, setStrokes] = useState<Stroke[]>([]);
  const [undone, setUndone] = useState<Stroke[]>([]);
  const [color, setColor] = useState(PALETTE[0]);
  const [width, setWidth] = useState(WIDTHS[0]);
  const [erase, setErase] = useState(false);
  const [title, setTitle] = useState('');
  const [saving, setSaving] = useState(false);
  const drawing = useRef<Stroke | null>(null);

  // Load existing note if editing
  useEffect(() => {
    if (!editingNoteId) return;
    api.listNotes().then(ns => {
      const n = ns.find(x => x.id === editingNoteId);
      if (n) { setStrokes(n.strokes); setTitle(n.title); }
    }).catch(() => {});
  }, [editingNoteId]);

  // Render
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const rect = canvas.getBoundingClientRect();
      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      draw(ctx, strokes);
    };
    resize();
    window.addEventListener('resize', resize);
    return () => window.removeEventListener('resize', resize);
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current; if (!canvas) return;
    const ctx = canvas.getContext('2d'); if (!ctx) return;
    const rect = canvas.getBoundingClientRect();
    ctx.clearRect(0, 0, rect.width, rect.height);
    draw(ctx, strokes);
  }, [strokes]);

  const posFromEvent = (e: React.PointerEvent): [number, number] => {
    const r = canvasRef.current!.getBoundingClientRect();
    return [e.clientX - r.left, e.clientY - r.top];
  };

  const onDown = (e: React.PointerEvent) => {
    (e.target as Element).setPointerCapture(e.pointerId);
    const s: Stroke = { color, width, erase, points: [posFromEvent(e)] };
    drawing.current = s;
    setStrokes([...strokes, s]);
    setUndone([]);
  };
  const onMove = (e: React.PointerEvent) => {
    if (!drawing.current) return;
    drawing.current.points.push(posFromEvent(e));
    setStrokes(s => [...s]); // force rerender; same array OK
  };
  const onUp = () => { drawing.current = null; };

  const save = async () => {
    setSaving(true);
    try {
      if (editingNoteId) {
        await api.updateNote(editingNoteId, { title, strokes });
      } else {
        await api.createNote({ subjectId: activeSubjectId, title, strokes });
      }
      setScreen('notes');
    } catch (e) { console.error(e); setSaving(false); }
  };

  const undo = () => { if (!strokes.length) return; const last = strokes[strokes.length - 1]; setStrokes(strokes.slice(0, -1)); setUndone([...undone, last]); };
  const redo = () => { if (!undone.length) return; const last = undone[undone.length - 1]; setUndone(undone.slice(0, -1)); setStrokes([...strokes, last]); };
  const clear = () => { setStrokes([]); setUndone([]); };

  return (
    <div className="min-h-screen flex flex-col px-4 pt-4">
      {/* Top bar */}
      <div className="flex items-center gap-2 mb-3">
        <button onClick={() => setScreen('notes')} className="text-sm text-white/70">← Back</button>
        <input
          value={title}
          onChange={e => setTitle(e.target.value.slice(0, 80))}
          placeholder="Title"
          className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm focus:outline-none placeholder:text-white/30"
        />
        <button
          onClick={save}
          disabled={saving}
          className="px-4 py-2 rounded-lg bg-violet-500 hover:bg-violet-400 text-sm font-medium disabled:opacity-50"
        >
          {saving ? 'Saving…' : 'Save'}
        </button>
      </div>

      {/* Canvas */}
      <div className="flex-1 min-h-0 rounded-2xl bg-gradient-to-br from-indigo-900/40 to-black border border-white/10 overflow-hidden relative">
        <canvas
          ref={canvasRef}
          onPointerDown={onDown}
          onPointerMove={onMove}
          onPointerUp={onUp}
          onPointerCancel={onUp}
          className="absolute inset-0 w-full h-full touch-none"
        />
      </div>

      {/* Toolbar */}
      <div className="py-3 space-y-3">
        <div className="flex gap-2">
          {PALETTE.map(c => (
            <button
              key={c}
              onClick={() => { setColor(c); setErase(false); }}
              aria-label={`color ${c}`}
              className={`w-9 h-9 rounded-full border-2 transition ${
                !erase && color === c ? 'border-white' : 'border-white/30'
              }`}
              style={{ background: c }}
            />
          ))}
        </div>
        <div className="flex items-center gap-2">
          {WIDTHS.map(w => (
            <button
              key={w}
              onClick={() => setWidth(w)}
              className={`w-10 h-10 rounded-lg border flex items-center justify-center ${
                width === w ? 'bg-white/15 border-violet-400' : 'bg-white/5 border-white/10'
              }`}
            >
              <span className="rounded-full bg-white" style={{ width: w, height: w }} />
            </button>
          ))}
          <div className="flex-1" />
          <ToolBtn selected={erase} onClick={() => setErase(!erase)}>Erase</ToolBtn>
          <ToolBtn onClick={undo}>Undo</ToolBtn>
          <ToolBtn onClick={redo}>Redo</ToolBtn>
          <ToolBtn onClick={clear}>Clear</ToolBtn>
        </div>
      </div>
    </div>
  );
}

function ToolBtn({ children, selected, onClick }: { children: React.ReactNode; selected?: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`px-3 py-2 rounded-lg text-xs border ${
        selected ? 'bg-violet-500/30 border-violet-400' : 'bg-white/5 border-white/10 hover:border-white/25'
      }`}
    >{children}</button>
  );
}

function draw(ctx: CanvasRenderingContext2D, strokes: Stroke[]) {
  ctx.lineCap = 'round'; ctx.lineJoin = 'round';
  for (const s of strokes) {
    if (s.points.length < 2) continue;
    ctx.strokeStyle = s.erase ? '#000' : s.color;
    ctx.lineWidth = s.width;
    ctx.beginPath();
    ctx.moveTo(s.points[0][0], s.points[0][1]);
    for (let i = 1; i < s.points.length; i++) ctx.lineTo(s.points[i][0], s.points[i][1]);
    ctx.stroke();
  }
}
