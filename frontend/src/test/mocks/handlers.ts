import { http, HttpResponse } from 'msw';
import { createSeasonsResponse, createTierListResponse } from '../fixtures';

export const handlers = [
  http.get('*/seasons', () => {
    return HttpResponse.json(createSeasonsResponse());
  }),

  http.get('*/tier-list', () => {
    return HttpResponse.json(createTierListResponse());
  }),
];
