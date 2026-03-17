import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { TierCard } from './TierCard';
import { createTier } from '@/test/fixtures';

describe('TierCard', () => {
  it('renders tier name and description', () => {
    const tier = createTier();

    render(<TierCard name="S" tier={tier} />);

    expect(screen.getByText('S')).toBeInTheDocument();
    expect(screen.getByText('Legends')).toBeInTheDocument();
  });

  it('renders driver cards', () => {
    const tier = createTier();

    render(<TierCard name="S" tier={tier} />);

    expect(screen.getByText('Lewis Hamilton')).toBeInTheDocument();
    expect(screen.getByText('Michael Schumacher')).toBeInTheDocument();
  });

  it('calls onDriverClick when clicking a driver card', async () => {
    const user = userEvent.setup();
    const onDriverClick = vi.fn();
    const tier = createTier();

    render(<TierCard name="S" tier={tier} onDriverClick={onDriverClick} />);

    await user.click(screen.getByText('Lewis Hamilton'));

    expect(onDriverClick).toHaveBeenCalledTimes(1);
    expect(onDriverClick).toHaveBeenCalledWith(tier.drivers[0]);
  });
});
