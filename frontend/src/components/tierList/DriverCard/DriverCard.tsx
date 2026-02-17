import type { Driver } from '@/api/types';
import { DriverInfo } from '../DriverInfo/DriverInfo';
import { StatBadges } from '../StatBadges/StatBadges';
import { RacesInfo } from '../RacesInfo/RacesInfo';
import styles from './DriverCard.module.scss';

interface DriverCardProps {
  driver: Driver;
  onClick?: (driver: Driver) => void;
}

export const DriverCard = ({ driver, onClick }: DriverCardProps) => {
  const { name, nationality, stats } = driver;

  const handleClick = () => {
    onClick?.(driver);
  };

  return (
    <div className={`${styles.card} ${onClick ? styles.clickable : ''}`} onClick={handleClick}>
      <DriverInfo name={name} nationality={nationality} />

      <div className={styles.statsSection}>
        <StatBadges stats={stats} compact />
        <RacesInfo races={stats.races} winRate={stats.win_rate} />
      </div>
    </div>
  );
};
