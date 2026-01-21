import {useEffect, useCallback, useState} from 'react';
import styles from './Modal.module.scss';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    children: React.ReactNode;
    title?: string;
}

export const Modal = ({isOpen, onClose, children, title}: ModalProps) => {
    const [isVisible, setIsVisible] = useState(false);
    const [isClosing, setIsClosing] = useState(false);

    const handleEscape = useCallback(
        (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                handleClose();
            }
        },
        []
    );

    const handleClose = () => {
        setIsClosing(true);
        setTimeout(() => {
            setIsClosing(false);
            setIsVisible(false);
            onClose();
        }, 200); // Длительность анимации
    };

    useEffect(() => {
        if (isOpen) {
            setIsVisible(true);
            document.addEventListener('keydown', handleEscape);

            const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
            document.body.style.paddingRight = `${scrollbarWidth}px`;
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
        };
    }, [isOpen, handleEscape]);

    useEffect(() => {
        if (!isVisible) {
            document.body.style.paddingRight = '';
            document.body.style.overflow = '';
        }
    }, [isVisible]);

    if (!isVisible) return null;

    return (
        <div
            className={`${styles.overlay} ${isClosing ? styles.closing : ''}`}
            onClick={handleClose}
        >
            <div
                className={`${styles.modal} ${isClosing ? styles.closing : ''}`}
                onClick={(e) => e.stopPropagation()}
            >
                <div className={styles.header}>
                    {title && <h2 className={styles.title}>{title}</h2>}
                    <button className={styles.closeButton} onClick={handleClose}>
                        ✕
                    </button>
                </div>
                <div className={styles.content}>{children}</div>
            </div>
        </div>
    );
};
