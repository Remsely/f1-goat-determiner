import type {Driver} from '@/api/types';
import {Modal} from '@/components/common/Modal/Modal';
import {StatBadges} from '../StatBadges/StatBadges';
import styles from './DriverModal.module.scss';

interface DriverModalProps {
    driver: Driver | null;
    isOpen: boolean;
    onClose: () => void;
}

interface StatRowProps {
    label: string;
    value: string | number;
}

const StatRow = ({label, value}: StatRowProps) => (
    <div className={styles.statRow}>
        <span className={styles.statLabel}>{label}</span>
        <span className={styles.statValue}>{value}</span>
    </div>
);

export const DriverModal = ({driver, isOpen, onClose}: DriverModalProps) => {
    if (!driver) return null;

    const {name, nationality, stats} = driver;

    const winsPerPodium = stats.podiums > 0
        ? ((stats.wins / stats.podiums) * 100).toFixed(1)
        : '0';

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={name}>
            <div className={styles.content}>
                <div className={styles.driverHeader}>
                    <span className={styles.nationality}>{nationality}</span>
                    <StatBadges stats={stats}/>
                </div>

                <div className={styles.section}>
                    <h3 className={styles.sectionTitle}>Career Stats</h3>
                    <div className={styles.statsGrid}>
                        <StatRow label="Races" value={stats.races}/>
                        <StatRow label="Championships" value={stats.titles}/>
                        <StatRow label="Wins" value={stats.wins}/>
                        <StatRow label="Pole Positions" value={stats.poles}/>
                        <StatRow label="Podiums" value={stats.podiums}/>
                    </div>
                </div>

                <div className={styles.section}>
                    <h3 className={styles.sectionTitle}>Performance</h3>
                    <div className={styles.statsGrid}>
                        <StatRow label="Win Rate" value={`${stats.win_rate.toFixed(2)}%`}/>
                        <StatRow label="Pole Rate" value={`${stats.pole_rate.toFixed(2)}%`}/>
                        <StatRow label="Podium Rate" value={`${stats.podium_rate.toFixed(2)}%`}/>
                        <StatRow
                            label="Avg.  Championship Position"
                            value={`${stats.avg_championship_pct.toFixed(1)}%`}
                        />
                        <StatRow label="Avg. Finish" value={stats.avg_finish.toFixed(2)}/>
                    </div>
                </div>

                {(stats.titles > 0 || stats.podiums > 0) && (
                    <div className={styles.section}>
                        <h3 className={styles.sectionTitle}>Additional Metrics</h3>
                        <div className={styles.statsGrid}>
                            {stats.titles > 0 && (
                                <StatRow label="Title Rate" value={`${stats.title_rate.toFixed(1)}%`}/>
                            )}
                            {stats.podiums > 0 && (
                                <StatRow label="Win/Podium Conversion" value={`${winsPerPodium}%`}/>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </Modal>
    );
};
