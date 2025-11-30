import styles from './StatBadge.module.scss';

export type BadgeType = 'titles' | 'wins' | 'podiums' | 'none';

interface StatBadgeProps {
    type: BadgeType;
    value?: number;
}

const BADGE_CONFIG: Record<BadgeType, { icon: string; label: string }> = {
    titles: {icon: '🏆', label: ''},
    wins: {icon: '🥇', label: 'W'},
    podiums: {icon: '🥉', label: 'P'},
    none: {icon: '', label: '—'},
};

export const StatBadge = ({type, value}: StatBadgeProps) => {
    const config = BADGE_CONFIG[type];

    if (type === 'none') {
        return <span className={styles.empty}>—</span>;
    }

    return (
        <span className={styles.badge} data-type={type}>
      <span className={styles.icon}>{config.icon}</span>
      <span className={styles.value}>{value}</span>
            {config.label && <span className={styles.label}>{config.label}</span>}
    </span>
    );
};
