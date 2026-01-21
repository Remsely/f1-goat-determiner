import type {Tier, Driver} from '@/api/types';
import {DriverCard} from '../DriverCard/DriverCard';
import {pluralize} from '@/utils/pluralize';
import styles from './TierCard.module.scss';

interface TierCardProps {
    name: string;
    tier: Tier;
    onDriverClick?: (driver: Driver) => void;
}

const TIER_DESCRIPTIONS: Record<string, string> = {
    S: 'Legends',
    A: 'Stars',
    B: 'Strong Drivers',
    C: 'Midfielders',
    D: 'Underperformers',
    F: 'Backmarkers',
};

export const TierCard = ({name, tier, onDriverClick}: TierCardProps) => {
    const description = TIER_DESCRIPTIONS[name] || name;
    const driversText = `${tier.count} ${pluralize(tier.count, 'driver', 'drivers')}`;

    return (
        <div className={styles.card} data-tier={name}>
            <div className={styles.header}>
                <div className={styles.titleSection}>
                    <div className={styles.label}>{name}</div>
                    <div className={styles.titleInfo}>
                        <div className={styles.name}>{description}</div>
                        <div className={styles.stats}>
                            <span>Avg.   wins: {tier.avg_win_rate}%</span>
                            <span> · </span>
                            <span>Avg.  finish: {tier.avg_finish?.toFixed(1) || 'N/A'}</span>
                        </div>
                    </div>
                </div>
                <div className={styles.count}>{driversText}</div>
            </div>

            <div className={styles.drivers}>
                {tier.drivers.map((driver) => (
                    <DriverCard
                        key={driver.id}
                        driver={driver}
                        onClick={onDriverClick}
                    />
                ))}
            </div>
        </div>
    );
};
