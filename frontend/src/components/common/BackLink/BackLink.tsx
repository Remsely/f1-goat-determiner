import {Link} from 'react-router-dom';
import styles from './BackLink.module.scss';
import React from "react";

interface BackLinkProps {
    to: string;
    children: React.ReactNode;
}

export const BackLink = ({to, children}: BackLinkProps) => {
    return (
        <Link to={to} className={styles.backLink}>
            {children}
        </Link>
    );
};
