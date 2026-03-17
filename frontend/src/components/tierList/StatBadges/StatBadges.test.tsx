import { render, screen } from '@testing-library/react';
import { StatBadges } from './StatBadges';
import { createDriverStats } from '@/test/fixtures';

describe('StatBadges', () => {
  it('renders badges for stats with non-zero values', () => {
    const stats = createDriverStats({ titles: 3, wins: 20, poles: 15, podiums: 50 });

    render(<StatBadges stats={stats} />);

    expect(screen.getByTitle('3 Championships')).toBeInTheDocument();
    expect(screen.getByTitle('20 Wins')).toBeInTheDocument();
    expect(screen.getByTitle('15 Pole Positions')).toBeInTheDocument();
    expect(screen.getByTitle('50 Podiums')).toBeInTheDocument();
  });

  it('renders "—" when all achievement stats are 0', () => {
    const stats = createDriverStats({ titles: 0, wins: 0, poles: 0, podiums: 0 });

    render(<StatBadges stats={stats} />);

    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
