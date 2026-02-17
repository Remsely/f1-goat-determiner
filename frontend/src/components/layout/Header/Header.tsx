import { NavLink } from 'react-router-dom';
import { Logo } from '../Logo/Logo';
import styles from './Header.module.scss';

interface NavItem {
  to: string;
  label: string;
  disabled?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/tier-list', label: 'Tier List' },
  { to: '/elo-rating', label: 'ELO Rating', disabled: true },
  { to: '/teammate-battles', label: 'Teammate Battles', disabled: true },
];

export const Header = () => {
  return (
    <header className={styles.header}>
      <div className={`container ${styles.content}`}>
        <Logo size="sm" />

        <nav className={styles.nav}>
          {NAV_ITEMS.map((item) =>
            item.disabled ? (
              <span key={item.to} className={styles.navItemDisabled}>
                {item.label}
              </span>
            ) : (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
              >
                {item.label}
              </NavLink>
            )
          )}
        </nav>
      </div>
    </header>
  );
};
