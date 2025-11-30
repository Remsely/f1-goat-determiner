export interface DriverStats {
    races: number;
    wins: number;
    podiums: number;
    titles: number;
    win_rate: number;
    podium_rate: number;
    title_rate: number;
    avg_championship_pct: number;
    avg_finish: number;
}

export interface Driver {
    id: number;
    ref: string;
    name: string;
    nationality: string;
    stats: DriverStats;
}

export interface Tier {
    count: number;
    avg_win_rate: number;
    avg_podium_rate: number;
    avg_finish: number;
    drivers: Driver[];
}

export interface TierListMeta {
    analyzer: string;
    seasons: number[] | null;
    n_tiers: number;
    min_races: number;
    total_drivers: number;
    silhouette_score: number;
}

export interface TierListResponse {
    meta: TierListMeta;
    tiers: Record<string, Tier>;
}

export interface SeasonsResponse {
    count: number;
    first: number;
    last: number;
    seasons: number[];
}

export interface DataStatsResponse {
    total_races: number;
    total_drivers: number;
    total_results: number;
    seasons: {
        first: number;
        last: number;
        count: number;
    };
}
