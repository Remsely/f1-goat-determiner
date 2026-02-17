import { Link } from 'react-router-dom';
import styles from './Logo.module.scss';
import logoImage from '@/assets/images/logo.png';

interface LogoProps {
  size?: 'sm' | 'md' | 'lg';
}

export const Logo = ({ size = 'md' }: LogoProps) => {
  return (
    <Link to="/" className={`${styles.logo} ${styles[size]}`}>
      <span className={styles.icon}>
        <img src={logoImage} alt="Logo" className={styles.iconImage} />
      </span>
      <span className={styles.title}>F1 GOAT Determiner</span>
    </Link>
  );
};
