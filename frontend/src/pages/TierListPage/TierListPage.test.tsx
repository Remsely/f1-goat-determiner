import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/mocks/server';
import { TierListPage } from './TierListPage';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/tier-list']}>
      <Routes>
        <Route path="/tier-list" element={<TierListPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('TierListPage', () => {
  it('shows loader while loading', () => {
    renderPage();
    expect(screen.getByText('Loading tier list...')).toBeInTheDocument();
  });

  it('renders tier cards after data loads', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.queryByText('Loading tier list...')).not.toBeInTheDocument();
    });

    expect(screen.getByText('Legends')).toBeInTheDocument();
    expect(screen.getByText('Stars')).toBeInTheDocument();
    expect(screen.getByText('Lewis Hamilton')).toBeInTheDocument();
  });

  it('shows metadata after loading', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.queryByText('Loading tier list...')).not.toBeInTheDocument();
    });

    expect(screen.getByText(/Drivers: 8/)).toBeInTheDocument();
    expect(screen.getByText(/Silhouette: 0.72/)).toBeInTheDocument();
  });

  it('opens driver modal on driver click', async () => {
    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.queryByText('Loading tier list...')).not.toBeInTheDocument();
    });

    await user.click(screen.getAllByText('Lewis Hamilton')[0]);

    expect(screen.getByText('Career Stats')).toBeInTheDocument();
    expect(screen.getByText('Performance')).toBeInTheDocument();
  });

  it('closes driver modal on close button click', async () => {
    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.queryByText('Loading tier list...')).not.toBeInTheDocument();
    });

    await user.click(screen.getAllByText('Lewis Hamilton')[0]);
    expect(screen.getByText('Career Stats')).toBeInTheDocument();

    await user.click(screen.getByText('✕'));

    await waitFor(() => {
      expect(screen.queryByText('Career Stats')).not.toBeInTheDocument();
    });
  });

  it('shows error on API failure', async () => {
    server.use(
      http.get('*/tier-list', () => {
        return HttpResponse.json({ detail: 'Something went wrong' }, { status: 500 });
      })
    );

    renderPage();

    await waitFor(() => {
      expect(screen.queryByText('Loading tier list...')).not.toBeInTheDocument();
    });

    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });
});
