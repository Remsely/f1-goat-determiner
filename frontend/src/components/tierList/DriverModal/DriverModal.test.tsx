import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { DriverModal } from './DriverModal';
import { createDriver, createDriverStats } from '@/test/fixtures';

describe('DriverModal', () => {
  it('renders nothing when driver is null', () => {
    const { container } = render(<DriverModal driver={null} isOpen={true} onClose={vi.fn()} />);
    expect(container.innerHTML).toBe('');
  });

  it('renders driver name, nationality, and stats', () => {
    const driver = createDriver();

    render(<DriverModal driver={driver} isOpen={true} onClose={vi.fn()} />);

    expect(screen.getByText('Lewis Hamilton')).toBeInTheDocument();
    expect(screen.getByText('British')).toBeInTheDocument();
    expect(screen.getByText('Career Stats')).toBeInTheDocument();
    expect(screen.getByText('Performance')).toBeInTheDocument();
    expect(screen.getByText('Races')).toBeInTheDocument();
    expect(screen.getByText('Championships')).toBeInTheDocument();
  });

  it('renders "Additional Metrics" when titles > 0 or podiums > 0', () => {
    const driver = createDriver({ stats: createDriverStats({ titles: 3, podiums: 50 }) });

    render(<DriverModal driver={driver} isOpen={true} onClose={vi.fn()} />);

    expect(screen.getByText('Additional Metrics')).toBeInTheDocument();
    expect(screen.getByText('Title Rate')).toBeInTheDocument();
    expect(screen.getByText('Win/Podium Conversion')).toBeInTheDocument();
  });

  it('does not render "Additional Metrics" when titles and podiums are 0', () => {
    const driver = createDriver({ stats: createDriverStats({ titles: 0, podiums: 0 }) });

    render(<DriverModal driver={driver} isOpen={true} onClose={vi.fn()} />);

    expect(screen.queryByText('Additional Metrics')).not.toBeInTheDocument();
  });

  it('correctly computes Win/Podium Conversion', () => {
    const driver = createDriver({
      stats: createDriverStats({ wins: 10, podiums: 40 }),
    });

    render(<DriverModal driver={driver} isOpen={true} onClose={vi.fn()} />);

    expect(screen.getByText('25.0%')).toBeInTheDocument();
  });
});
