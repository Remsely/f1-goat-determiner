import { pluralize } from '@/utils/pluralize';
import styles from './RacesInfo.module.scss';

interface RacesInfoProps {
  races: number;
  winRate: number;
}

export const RacesInfo = ({ races, winRate }: RacesInfoProps) => {
  const racesText = `${races} ${pluralize(races, 'race', 'races')}`;

  return (
    <div className={styles.races}>
      {racesText}
      {winRate > 0 && (
        <>
          <span className={styles.separator}>·</span>
          <span className={styles.winRate}>{winRate.toFixed(1)}% wins</span>
        </>
      )}
    </div>
  );
};
