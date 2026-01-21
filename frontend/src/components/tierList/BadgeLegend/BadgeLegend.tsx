import styles from './BadgeLegend.module.scss';

const LEGEND_ITEMS = [
    {icon: '🏆', label: 'Championships', type: 'titles'},
    {icon: '🥇', label: 'Wins', type: 'wins'},
    {icon: '⚡', label: 'Poles', type: 'poles'},
    {icon: '🥉', label: 'Podiums', type: 'podiums'},
];

export const BadgeLegend = () => {
    return (
        <div className={styles.legend}>
            {LEGEND_ITEMS.map((item) => (
                <div key={item.type} className={styles.item} data-type={item.type}>
                    <span className={styles.icon}>{item.icon}</span>
                    <span className={styles.label}>{item.label}</span>
                </div>
            ))}
        </div>
    );
};
