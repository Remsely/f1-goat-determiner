import styles from './Loader.module.scss';

interface LoaderProps {
  size?: 'sm' | 'md' | 'lg';
  text?: string;
}

export const Loader = ({ size = 'md', text }: LoaderProps) => {
  return (
    <div className={styles.wrapper}>
      <div className={`${styles.spinner} ${styles[size]}`}>
        <div className={styles.ring}></div>
        <div className={styles.ring}></div>
        <div className={styles.ring}></div>
      </div>
      {text && <span className={styles.text}>{text}</span>}
    </div>
  );
};
