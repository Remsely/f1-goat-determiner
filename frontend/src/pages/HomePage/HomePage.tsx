import {Link} from 'react-router-dom';
import styles from './HomePage.module.scss';

export const HomePage = () => {
    return (
        <div className={styles.page}>
            <div className="container">
                <h1 className={styles.title}>F1 GOAT Determiner</h1>
                <p className={styles.subtitle}>
                    Find out who is the greatest Formula 1 driver of all time
                </p>

                <div className={styles.features}>
                    <Link to="/tier-list" className={styles.featureCard}>
                        <h2>🏆 Tier List</h2>
                        <p>Driver clustering by skill level using K-Means algorithm</p>
                    </Link>

                    <div className={`${styles.featureCard} ${styles.disabled}`}>
                        <h2>📊 ELO Rating</h2>
                        <p>Coming soon...</p>
                    </div>
                </div>
            </div>
        </div>
    );
};
