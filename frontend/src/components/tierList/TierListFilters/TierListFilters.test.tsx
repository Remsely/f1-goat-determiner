import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { TierListFilters } from './TierListFilters';

const availableSeasons = Array.from({ length: 75 }, (_, i) => 1950 + i);

describe('TierListFilters', () => {
  const defaultProps = {
    availableSeasons,
    selectedSeasons: [] as number[],
    onSeasonsChange: vi.fn(),
    nTiers: 4,
    onNTiersChange: vi.fn(),
    minRaces: 10,
    onMinRacesChange: vi.fn(),
  };

  it('renders era presets, tier select, and min races select', () => {
    render(<TierListFilters {...defaultProps} />);

    expect(screen.getByText('Era')).toBeInTheDocument();
    expect(screen.getByText('All Seasons')).toBeInTheDocument();
    expect(screen.getByText('Number of Tiers')).toBeInTheDocument();
    expect(screen.getByText('Minimum Races')).toBeInTheDocument();
  });

  it('shows SeasonPicker when clicking "Select Seasons..."', async () => {
    const user = userEvent.setup();

    render(<TierListFilters {...defaultProps} />);

    expect(screen.queryByText('Select All')).not.toBeInTheDocument();

    await user.click(screen.getByText('Select Seasons...'));

    expect(screen.getByText('Select All')).toBeInTheDocument();
  });

  it('hides SeasonPicker when selecting a preset', async () => {
    const user = userEvent.setup();

    render(<TierListFilters {...defaultProps} />);

    await user.click(screen.getByText('Select Seasons...'));
    expect(screen.getByText('Select All')).toBeInTheDocument();

    await user.click(screen.getByText('All Seasons'));
    expect(screen.queryByText('Select All')).not.toBeInTheDocument();
  });

  it('calls onNTiersChange when tier count changes', async () => {
    const user = userEvent.setup();
    const onNTiersChange = vi.fn();

    render(<TierListFilters {...defaultProps} onNTiersChange={onNTiersChange} />);

    const tierSelect = screen.getByText('Number of Tiers').parentElement!.querySelector('select')!;
    await user.selectOptions(tierSelect, '5');

    expect(onNTiersChange).toHaveBeenCalledWith(5);
  });

  it('calls onMinRacesChange when min races changes', async () => {
    const user = userEvent.setup();
    const onMinRacesChange = vi.fn();

    render(<TierListFilters {...defaultProps} onMinRacesChange={onMinRacesChange} />);

    const minRacesSelect = screen.getByText('Minimum Races').parentElement!.querySelector('select')!;
    await user.selectOptions(minRacesSelect, '50');

    expect(onMinRacesChange).toHaveBeenCalledWith(50);
  });
});
