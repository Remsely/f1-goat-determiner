import { pluralize } from '@/utils/pluralize';
import styles from './SeasonPicker.module.scss';

interface SeasonPickerProps {
  availableSeasons: number[];
  selectedSeasons: number[];
  onToggleSeason: (year: number) => void;
  onSelectAll: () => void;
  onClear: () => void;
}

export const SeasonPicker = ({
  availableSeasons,
  selectedSeasons,
  onToggleSeason,
  onSelectAll,
  onClear,
}: SeasonPickerProps) => {
  const selectedCount = selectedSeasons.length;
  const seasonWord = pluralize(selectedCount, 'season', 'seasons');

  return (
    <div className={styles.picker}>
      <div className={styles.header}>
        <span>Select seasons:</span>
        <div className={styles.actions}>
          <button onClick={onSelectAll}>Select All</button>
          <button onClick={onClear}>Clear</button>
        </div>
      </div>

      <div className={styles.grid}>
        {availableSeasons.map((year) => (
          <button
            key={year}
            className={`${styles.chip} ${selectedSeasons.includes(year) ? styles.selected : ''}`}
            onClick={() => onToggleSeason(year)}
          >
            {year}
          </button>
        ))}
      </div>

      {selectedCount > 0 && (
        <div className={styles.count}>
          Selected: {selectedCount} {seasonWord}
        </div>
      )}
    </div>
  );
};
