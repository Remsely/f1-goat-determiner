import {Link} from 'react-router-dom';
import styles from './HomePage.module.scss';

export const HomePage = () => {
    return (
        <div className={styles.page}>
            <div className="container">
                <h1 className={styles.title}>F1 GOAT Determiner</h1>
                <p className={styles.subtitle}>
                    Определи лучшего пилота Формулы-1 всех времён
                </p>

                <div className={styles.features}>
                    <Link to="/tier-list" className={styles.featureCard}>
                        <h2>🏆 Tier List</h2>
                        <p>Кластеризация пилотов по уровню мастерства</p>
                    </Link>

                    <div className={styles.featureCard + ' ' + styles.disabled}>
                        <h2>📊 ELO Rating</h2>
                        <p>Скоро... </p>
                    </div>

                    <div className={styles.featureCard + ' ' + styles.disabled}>
                        <h2>🤝 Teammate Battles</h2>
                        <p>Скоро...</p>
                    </div>
                </div>
            </div>
        </div>
    );
};
