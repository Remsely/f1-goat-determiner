import {StatBadge} from '../StatBadge/StatBadge';
import type {DriverStats} from '@/api/types';
import styles from './StatBadges.module.scss';

interface StatBadgesProps {
    stats: DriverStats;
}

export const StatBadges = ({stats}: StatBadgesProps) => {
    const hasAchievements = stats.wins > 0 || stats.podiums > 0 || stats.titles > 0;

    if (!hasAchievements) {
        return (
            <div className={styles.badges}>
                <StatBadge type="none"/>
            </div>
        );
    }

    return (
        <div className={styles.badges}>
            {stats.titles > 0 && <StatBadge type="titles" value={stats.titles}/>}
            {stats.wins > 0 && <StatBadge type="wins" value={stats.wins}/>}
            {stats.podiums > 0 && <StatBadge type="podiums" value={stats.podiums}/>}
        </div>
    );
};
