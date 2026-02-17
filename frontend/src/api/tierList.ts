import { apiClient } from './client';
import type { TierListResponse, SeasonsResponse } from './types';

export interface TierListParams {
  seasons?: number[];
  nTiers?: number;
  minRaces?: number;
}

export const tierListApi = {
  async getSeasons(): Promise<SeasonsResponse> {
    const { data } = await apiClient.get<SeasonsResponse>('/seasons');
    return data;
  },

  async getTierList(params: TierListParams = {}): Promise<TierListResponse> {
    const queryParams: Record<string, string> = {};

    if (params.seasons && params.seasons.length > 0) {
      queryParams.seasons = params.seasons.join(',');
    }
    if (params.nTiers) {
      queryParams.n_tiers = params.nTiers.toString();
    }
    if (params.minRaces) {
      queryParams.min_races = params.minRaces.toString();
    }

    const { data } = await apiClient.get<TierListResponse>('/tier-list', {
      params: queryParams,
    });
    return data;
  },
};
