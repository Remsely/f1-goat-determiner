import type {Driver} from '@/api/types';
import {DriverInfo} from '../DriverInfo/DriverInfo';
import {StatBadges} from '../StatBadges/StatBadges';
import {RacesInfo} from '../RacesInfo/RacesInfo';
import styles from './DriverCard.module.scss';

interface DriverCardProps {
    driver: Driver;
}

export const DriverCard = ({driver}: DriverCardProps) => {
    const {name, nationality, stats} = driver;

    return (
        <div className={styles.card}>
            <DriverInfo name={name} nationality={nationality}/>

            <div className={styles.statsSection}>
                <StatBadges stats={stats}/>
                <RacesInfo races={stats.races} winRate={stats.win_rate}/>
            </div>
        </div>
    );
};
