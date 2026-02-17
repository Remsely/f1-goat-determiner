import { Outlet } from 'react-router-dom';
import { Header } from '../Header/Header';
import styles from './Layout.module.scss';

interface LayoutProps {
  showHeader?: boolean;
}

export const Layout = ({ showHeader = true }: LayoutProps) => {
  return (
    <div className={styles.layout}>
      {showHeader && <Header />}
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
};
