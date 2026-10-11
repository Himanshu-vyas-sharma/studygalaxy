function hash(str: string): number {
  let h = 2166136261;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

export function gradientFromSeed(seed: string): string {
  const h = hash(seed);
  const a = h % 360;
  const b = (a + 60 + (h >> 8) % 120) % 360;
  return `linear-gradient(135deg, hsl(${a} 85% 60%) 0%, hsl(${b} 85% 50%) 100%)`;
}

export function initialsOf(name: string): string {
  const parts = name.replace(/[^A-Za-z0-9 ]/g, '').split(/(?=[A-Z])|\s+/).filter(Boolean);
  const first = parts[0]?.[0] ?? name[0] ?? '*';
  const second = parts[1]?.[0] ?? parts[0]?.[1] ?? '';
  return (first + second).toUpperCase();
}
