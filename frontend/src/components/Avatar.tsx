import { gradientFromSeed, initialsOf } from '../lib/avatar';

interface Props {
  seed: string;
  name: string;
  size?: number;
}

export function Avatar({ seed, name, size = 44 }: Props) {
  return (
    <div
      className="flex items-center justify-center rounded-full font-semibold text-white/95 ring-2 ring-white/10 shadow-lg select-none"
      style={{
        width: size,
        height: size,
        background: gradientFromSeed(seed),
        fontSize: size * 0.4,
      }}
    >
      {initialsOf(name)}
    </div>
  );
}
