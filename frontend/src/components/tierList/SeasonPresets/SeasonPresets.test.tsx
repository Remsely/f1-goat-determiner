import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { SeasonPresets } from './SeasonPresets';

const availableSeasons = Array.from({ length: 75 }, (_, i) => 1950 + i);

describe('SeasonPresets', () => {
  it('renders all presets and custom button', () => {
    render(
      <SeasonPresets
        activePreset="all"
        availableSeasons={availableSeasons}
        onPresetSelect={vi.fn()}
        onCustomClick={vi.fn()}
        isCustomActive={false}
      />
    );

    expect(screen.getByText('All Seasons')).toBeInTheDocument();
    expect(screen.getByText('Hybrid Era (2014+)')).toBeInTheDocument();
    expect(screen.getByText('2010s')).toBeInTheDocument();
    expect(screen.getByText('1950s')).toBeInTheDocument();
    expect(screen.getByText('Select Seasons...')).toBeInTheDocument();
    expect(screen.getAllByRole('button')).toHaveLength(13); // 12 preset buttons + 1 "Select Seasons..." custom button
  });

  it('"All Seasons" calls onPresetSelect with empty array', async () => {
    const user = userEvent.setup();
    const onPresetSelect = vi.fn();

    render(
      <SeasonPresets
        activePreset=""
        availableSeasons={availableSeasons}
        onPresetSelect={onPresetSelect}
        onCustomClick={vi.fn()}
        isCustomActive={false}
      />
    );

    await user.click(screen.getByText('All Seasons'));
    expect(onPresetSelect).toHaveBeenCalledWith('all', []);
  });

  it('"Hybrid Era" filters seasons >= 2014', async () => {
    const user = userEvent.setup();
    const onPresetSelect = vi.fn();

    render(
      <SeasonPresets
        activePreset=""
        availableSeasons={availableSeasons}
        onPresetSelect={onPresetSelect}
        onCustomClick={vi.fn()}
        isCustomActive={false}
      />
    );

    await user.click(screen.getByText('Hybrid Era (2014+)'));
    const seasons = onPresetSelect.mock.calls[0][1] as number[];
    expect(seasons.every((y: number) => y >= 2014)).toBe(true);
    expect(seasons).toContain(2014);
    expect(seasons).not.toContain(2013);
  });

  it('decade preset filters correctly (2010s = [2010..2019])', async () => {
    const user = userEvent.setup();
    const onPresetSelect = vi.fn();

    render(
      <SeasonPresets
        activePreset=""
        availableSeasons={availableSeasons}
        onPresetSelect={onPresetSelect}
        onCustomClick={vi.fn()}
        isCustomActive={false}
      />
    );

    await user.click(screen.getByText('2010s'));
    const seasons = onPresetSelect.mock.calls[0][1] as number[];
    expect(seasons).toEqual([2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019]);
  });

  it('"Select Seasons..." calls onCustomClick', async () => {
    const user = userEvent.setup();
    const onCustomClick = vi.fn();

    render(
      <SeasonPresets
        activePreset=""
        availableSeasons={availableSeasons}
        onPresetSelect={vi.fn()}
        onCustomClick={onCustomClick}
        isCustomActive={false}
      />
    );

    await user.click(screen.getByText('Select Seasons...'));
    expect(onCustomClick).toHaveBeenCalledTimes(1);
  });

  it('active preset has active style', () => {
    render(
      <SeasonPresets
        activePreset="all"
        availableSeasons={availableSeasons}
        onPresetSelect={vi.fn()}
        onCustomClick={vi.fn()}
        isCustomActive={false}
      />
    );

    const allButton = screen.getByText('All Seasons');
    expect(allButton.className).toContain('active');

    const hybridButton = screen.getByText('Hybrid Era (2014+)');
    expect(hybridButton.className).not.toContain('active');
  });
});
