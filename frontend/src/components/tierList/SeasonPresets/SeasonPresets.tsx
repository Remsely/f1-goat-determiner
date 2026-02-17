import styles from './SeasonPresets.module.scss';

export interface Preset {
  id: string;
  label: string;
  filter?: (year: number, currentYear: number) => boolean;
}

const PRESETS: Preset[] = [
  { id: 'all', label: 'All Seasons' },
  { id: 'modern', label: 'Hybrid Era (2014+)', filter: (y) => y >= 2014 },
  { id: 'last5', label: 'Last 5 Years', filter: (y, curr) => y > curr - 5 },
  { id: 'last10', label: 'Last 10 Years', filter: (y, curr) => y > curr - 10 },
  { id: '2020s', label: '2020s', filter: (y) => y >= 2020 },
  { id: '2010s', label: '2010s', filter: (y) => y >= 2010 && y < 2020 },
  { id: '2000s', label: '2000s', filter: (y) => y >= 2000 && y < 2010 },
  { id: '1990s', label: '1990s', filter: (y) => y >= 1990 && y < 2000 },
  { id: '1980s', label: '1980s', filter: (y) => y >= 1980 && y < 1990 },
  { id: '1970s', label: '1970s', filter: (y) => y >= 1970 && y < 1980 },
  { id: '1960s', label: '1960s', filter: (y) => y >= 1960 && y < 1970 },
  { id: '1950s', label: '1950s', filter: (y) => y >= 1950 && y < 1960 },
];

interface SeasonPresetsProps {
  activePreset: string;
  availableSeasons: number[];
  onPresetSelect: (presetId: string, seasons: number[]) => void;
  onCustomClick: () => void;
  isCustomActive: boolean;
}

export const SeasonPresets = ({
  activePreset,
  availableSeasons,
  onPresetSelect,
  onCustomClick,
  isCustomActive,
}: SeasonPresetsProps) => {
  const currentYear = new Date().getFullYear();

  const handlePresetClick = (preset: Preset) => {
    if (preset.id === 'all') {
      onPresetSelect('all', []);
    } else if (preset.filter) {
      const filtered = availableSeasons.filter((y) => preset.filter!(y, currentYear));
      onPresetSelect(preset.id, filtered);
    }
  };

  return (
    <div className={styles.presets}>
      {PRESETS.map((preset) => (
        <button
          key={preset.id}
          className={`${styles.preset} ${activePreset === preset.id ? styles.active : ''}`}
          onClick={() => handlePresetClick(preset)}
        >
          {preset.label}
        </button>
      ))}
      <button
        className={`${styles.preset} ${styles.custom} ${isCustomActive ? styles.active : ''}`}
        onClick={onCustomClick}
      >
        Select Seasons...
      </button>
    </div>
  );
};
