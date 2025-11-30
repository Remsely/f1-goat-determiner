import styles from './DriverInfo.module.scss';

interface DriverInfoProps {
    name: string;
    nationality: string;
}

export const DriverInfo = ({name, nationality}: DriverInfoProps) => {
    return (
        <div className={styles.info}>
            <span className={styles.name}>{name}</span>
            <span className={styles.nationality}>{nationality}</span>
        </div>
    );
};
