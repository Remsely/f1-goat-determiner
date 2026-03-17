import { renderHook, waitFor, act } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/mocks/server';
import { useTierList } from './useTierList';
import { createTierListResponse } from '@/test/fixtures';

describe('useTierList', () => {
  it('loads available seasons on mount', async () => {
    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.availableSeasons.length).toBe(75);
    });
    expect(result.current.availableSeasons[0]).toBe(1950);
  });

  it('loads tier list on mount with defaults', async () => {
    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.data).not.toBeNull();
    expect(result.current.data?.meta.n_tiers).toBe(4);
    expect(result.current.nTiers).toBe(4);
    expect(result.current.minRaces).toBe(10);
  });

  it('reloads tier list when selectedSeasons change', async () => {
    let callCount = 0;
    server.use(
      http.get('*/tier-list', () => {
        callCount++;
        return HttpResponse.json(createTierListResponse());
      })
    );

    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const initialCount = callCount;

    act(() => {
      result.current.setSelectedSeasons([2020, 2021]);
    });

    await waitFor(() => {
      expect(callCount).toBe(initialCount + 1);
    });
  });

  it('reloads tier list when nTiers changes', async () => {
    let callCount = 0;
    server.use(
      http.get('*/tier-list', () => {
        callCount++;
        return HttpResponse.json(createTierListResponse());
      })
    );

    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const initialCount = callCount;

    act(() => {
      result.current.setNTiers(5);
    });

    await waitFor(() => {
      expect(callCount).toBe(initialCount + 1);
    });
  });

  it('reloads tier list when minRaces changes', async () => {
    let callCount = 0;
    server.use(
      http.get('*/tier-list', () => {
        callCount++;
        return HttpResponse.json(createTierListResponse());
      })
    );

    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const initialCount = callCount;

    act(() => {
      result.current.setMinRaces(50);
    });

    await waitFor(() => {
      expect(callCount).toBe(initialCount + 1);
    });
  });

  it('sets error when seasons API returns 500', async () => {
    server.use(
      http.get('*/seasons', () => {
        return HttpResponse.json({ detail: 'Seasons unavailable' }, { status: 500 });
      })
    );

    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.error).not.toBeNull();
    });
    expect(result.current.error).toBe('Failed to load available seasons');
  });

  it('sets error when API returns 500', async () => {
    server.use(
      http.get('*/tier-list', () => {
        return HttpResponse.json({ detail: 'Internal server error' }, { status: 500 });
      })
    );

    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBe('Internal server error');
  });

  it('falls back to generic error message when detail is missing', async () => {
    server.use(
      http.get('*/tier-list', () => {
        return HttpResponse.json({}, { status: 500 });
      })
    );

    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBe('Failed to load data');
  });

  it('sets loading to true during fetch and false after', async () => {
    const { result } = renderHook(() => useTierList());

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
  });

  it('refresh reloads data', async () => {
    let callCount = 0;
    server.use(
      http.get('*/tier-list', () => {
        callCount++;
        return HttpResponse.json(createTierListResponse());
      })
    );

    const { result } = renderHook(() => useTierList());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const countBefore = callCount;

    act(() => {
      result.current.refresh();
    });

    await waitFor(() => {
      expect(callCount).toBe(countBefore + 1);
    });
  });

  it('accepts initialNTiers and initialMinRaces via options', async () => {
    const { result } = renderHook(() => useTierList({ initialNTiers: 6, initialMinRaces: 50 }));

    expect(result.current.nTiers).toBe(6);
    expect(result.current.minRaces).toBe(50);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
  });
});
