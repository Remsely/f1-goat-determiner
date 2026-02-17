import { Link } from 'react-router-dom';
import styles from './HomePage.module.scss';

import logoImage from '@/assets/images/logo.png';

export const HomePage = () => {
  return (
    <div className={styles.page}>
      <div className="container">
        <div className={styles.hero}>
          <div className={styles.logoIcon}>
            <img src={logoImage} alt="Logo" />
          </div>

          <h1 className={styles.title}>
            <span className={styles.titleMain}>F1 GOAT</span>
            <span className={styles.titleAccent}>Determiner</span>
          </h1>

          <p className={styles.subtitle}>Find out who is the greatest Formula 1 driver of all time</p>
        </div>

        <div className={styles.features}>
          <Link to="/tier-list" className={styles.featureCard}>
            <div className={styles.featureIcon}>🏆</div>
            <div className={styles.featureContent}>
              <h2>Tier List</h2>
              <p>Driver clustering by skill level using K-Means algorithm</p>
            </div>
          </Link>

          <div className={`${styles.featureCard} ${styles.disabled}`}>
            <div className={styles.featureIcon}>📊</div>
            <div className={styles.featureContent}>
              <h2>ELO Rating</h2>
              <p>Coming soon...</p>
            </div>
          </div>

          <div className={`${styles.featureCard} ${styles.disabled}`}>
            <div className={styles.featureIcon}>🤝</div>
            <div className={styles.featureContent}>
              <h2>Teammate Battles</h2>
              <p>Coming soon...</p>
            </div>
          </div>
        </div>

        <footer className={styles.footer}>
          <p>Data from 1950 to 2024 · Powered by Kaggle F1 Dataset</p>
        </footer>
      </div>
    </div>
  );
};
