import {useEffect, useState, useCallback} from 'react';
import {tierListApi} from '@/api/tierList';
import type {TierListResponse} from '@/api/types';

export interface UseTierListOptions {
    initialNTiers?: number;
    initialMinRaces?: number;
}

export interface UseTierListReturn {
    data: TierListResponse | null;
    loading: boolean;
    error: string | null;

    availableSeasons: number[];
    selectedSeasons: number[];
    setSelectedSeasons: (seasons: number[]) => void;

    nTiers: number;
    setNTiers: (n: number) => void;
    minRaces: number;
    setMinRaces: (n: number) => void;

    refresh: () => void;
}

export function useTierList(options: UseTierListOptions = {}): UseTierListReturn {
    const {initialNTiers = 4, initialMinRaces = 10} = options;

    const [data, setData] = useState<TierListResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [availableSeasons, setAvailableSeasons] = useState<number[]>([]);
    const [selectedSeasons, setSelectedSeasons] = useState<number[]>([]);
    const [nTiers, setNTiers] = useState(initialNTiers);
    const [minRaces, setMinRaces] = useState(initialMinRaces);

    useEffect(() => {
        tierListApi.getSeasons().then((res) => {
            setAvailableSeasons(res.seasons);
        });
    }, []);

    const loadTierList = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const result = await tierListApi.getTierList({
                seasons: selectedSeasons.length > 0 ? selectedSeasons : undefined,
                nTiers,
                minRaces,
            });
            setData(result);
        } catch (err) {
            setError('Failed to load data');
            console.error(err);
        } finally {
            setLoading(false);
        }
    }, [selectedSeasons, nTiers, minRaces]);

    useEffect(() => {
        loadTierList();
    }, [loadTierList]);

    return {
        data,
        loading,
        error,
        availableSeasons,
        selectedSeasons,
        setSelectedSeasons,
        nTiers,
        setNTiers,
        minRaces,
        setMinRaces,
        refresh: loadTierList,
    };
}
