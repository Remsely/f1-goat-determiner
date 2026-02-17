import styles from './StatBadge.module.scss';

export type BadgeType = 'titles' | 'wins' | 'podiums' | 'poles' | 'none';

interface StatBadgeProps {
  type: BadgeType;
  value?: number;
  compact?: boolean;
}

const BADGE_CONFIG: Record<BadgeType, { icon: string; title: string }> = {
  titles: { icon: '🏆', title: 'Championships' },
  wins: { icon: '🥇', title: 'Wins' },
  poles: { icon: '⚡', title: 'Pole Positions' },
  podiums: { icon: '🥉', title: 'Podiums' },
  none: { icon: '', title: '' },
};

export const StatBadge = ({ type, value, compact = false }: StatBadgeProps) => {
  const config = BADGE_CONFIG[type];

  if (type === 'none') {
    return <span className={styles.empty}>—</span>;
  }

  return (
    <span
      className={`${styles.badge} ${compact ? styles.compact : ''}`}
      data-type={type}
      title={`${value} ${config.title}`}
    >
      <span className={styles.icon}>{config.icon}</span>
      <span className={styles.value}>{value}</span>
    </span>
  );
};
