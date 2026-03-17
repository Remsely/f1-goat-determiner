import type { Driver, DriverStats, Tier, TierListResponse, SeasonsResponse } from '@/api/types';

export function createDriverStats(overrides: Partial<DriverStats> = {}): DriverStats {
  return {
    races: 100,
    wins: 20,
    podiums: 50,
    poles: 15,
    titles: 3,
    win_rate: 20.0,
    podium_rate: 50.0,
    pole_rate: 15.0,
    title_rate: 3.0,
    avg_championship_pct: 85.0,
    avg_finish: 4.5,
    ...overrides,
  };
}

export function createDriver(overrides: Partial<Driver> = {}): Driver {
  return {
    id: 1,
    ref: 'hamilton',
    name: 'Lewis Hamilton',
    nationality: 'British',
    stats: createDriverStats(),
    ...overrides,
  };
}

export function createTier(overrides: Partial<Tier> = {}): Tier {
  return {
    count: 2,
    avg_win_rate: 25.0,
    avg_podium_rate: 55.0,
    avg_pole_rate: 18.0,
    avg_finish: 3.5,
    drivers: [
      createDriver(),
      createDriver({
        id: 2,
        ref: 'schumacher',
        name: 'Michael Schumacher',
        nationality: 'German',
        stats: createDriverStats({ wins: 91, titles: 7 }),
      }),
    ],
    ...overrides,
  };
}

export function createTierListResponse(): TierListResponse {
  return {
    meta: {
      analyzer: 'kmeans',
      seasons: [2010, 2011, 2012, 2013, 2014],
      n_tiers: 4,
      min_races: 10,
      total_drivers: 8,
      silhouette_score: 0.72,
    },
    tiers: {
      S: createTier(),
      A: createTier({
        count: 3,
        avg_win_rate: 10.0,
        avg_finish: 5.0,
        drivers: [
          createDriver({
            id: 3,
            ref: 'vettel',
            name: 'Sebastian Vettel',
            nationality: 'German',
            stats: createDriverStats({ wins: 53, titles: 4 }),
          }),
          createDriver({
            id: 4,
            ref: 'alonso',
            name: 'Fernando Alonso',
            nationality: 'Spanish',
            stats: createDriverStats({ wins: 32, titles: 2 }),
          }),
          createDriver({
            id: 5,
            ref: 'raikkonen',
            name: 'Kimi Räikkönen',
            nationality: 'Finnish',
            stats: createDriverStats({ wins: 21, titles: 1 }),
          }),
        ],
      }),
      B: createTier({
        count: 2,
        avg_win_rate: 3.0,
        avg_finish: 8.0,
        drivers: [
          createDriver({
            id: 6,
            ref: 'button',
            name: 'Jenson Button',
            nationality: 'British',
            stats: createDriverStats({ wins: 15, titles: 1, poles: 8 }),
          }),
          createDriver({
            id: 7,
            ref: 'rosberg',
            name: 'Nico Rosberg',
            nationality: 'German',
            stats: createDriverStats({ wins: 23, titles: 1, poles: 30 }),
          }),
        ],
      }),
      C: createTier({
        count: 1,
        avg_win_rate: 0.5,
        avg_finish: 12.0,
        drivers: [
          createDriver({
            id: 8,
            ref: 'hulkenberg',
            name: 'Nico Hülkenberg',
            nationality: 'German',
            stats: createDriverStats({ wins: 0, titles: 0, poles: 1, podiums: 0 }),
          }),
        ],
      }),
    },
  };
}

export function createSeasonsResponse(): SeasonsResponse {
  return {
    count: 75,
    first: 1950,
    last: 2024,
    seasons: Array.from({ length: 75 }, (_, i) => 1950 + i),
  };
}
