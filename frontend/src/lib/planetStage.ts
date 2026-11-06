/**
 * A subject's planet terraforms as focused minutes accumulate. Must stay in
 * step with android-native .../util/PlanetStage.kt so the same planet looks the
 * same on both clients.
 */
export interface PlanetStage {
  label: string;
  minMinutes: number;
  atmosphere: boolean;
  water: boolean;
  rings: boolean;
  moons: number;
  life: boolean;
}

export const PLANET_STAGES: PlanetStage[] = [
  { label: 'Barren rock',       minMinutes: 0,    atmosphere: false, water: false, rings: false, moons: 0, life: false },
  { label: 'Cooling crust',     minMinutes: 60,   atmosphere: false, water: false, rings: false, moons: 0, life: false },
  { label: 'Atmosphere forming',minMinutes: 180,  atmosphere: true,  water: false, rings: false, moons: 0, life: false },
  { label: 'Oceans',            minMinutes: 420,  atmosphere: true,  water: true,  rings: false, moons: 0, life: false },
  { label: 'Ringed world',      minMinutes: 900,  atmosphere: true,  water: true,  rings: true,  moons: 0, life: false },
  { label: 'Moons captured',    minMinutes: 1800, atmosphere: true,  water: true,  rings: true,  moons: 2, life: false },
  { label: 'Life',              minMinutes: 3600, atmosphere: true,  water: true,  rings: true,  moons: 3, life: true  },
];

export function stageFor(totalMin: number): PlanetStage {
  let out = PLANET_STAGES[0];
  for (const s of PLANET_STAGES) if (totalMin >= s.minMinutes) out = s;
  return out;
}

export function nextStage(totalMin: number): PlanetStage | null {
  return PLANET_STAGES.find(s => s.minMinutes > totalMin) ?? null;
}

export function minutesToNext(totalMin: number): number | null {
  const next = nextStage(totalMin);
  return next ? next.minMinutes - totalMin : null;
}
