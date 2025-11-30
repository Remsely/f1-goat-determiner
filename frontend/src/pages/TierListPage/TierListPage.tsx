import {useState} from 'react';
import {Loader} from '@/components/common/Loader/Loader';
import {TierListFilters} from '@/components/tierList/TierListFilters/TierListFilters';
import {TierCard} from '@/components/tierList/TierCard/TierCard';
import {BadgeLegend} from '@/components/tierList/BadgeLegend/BadgeLegend';
import {DriverModal} from '@/components/tierList/DriverModal/DriverModal';
import {useTierList} from '@/hooks/useTierList';
import type {Driver} from '@/api/types';
import styles from './TierListPage.module.scss';

export const TierListPage = () => {
    const {
        data,
        loading,
        error,
        availableSeasons,
        selectedSeasons,
        setSelectedSeasons,
        nTiers,
        setNTiers,
        minRaces,
        setMinRaces,
    } = useTierList();

    const [selectedDriver, setSelectedDriver] = useState<Driver | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const handleDriverClick = (driver: Driver) => {
        setSelectedDriver(driver);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setSelectedDriver(null);
    };

    return (
        <div className={styles.page}>
            <div className="container">
                <header className={styles.header}>
                    <h1 className={styles.title}>Tier List</h1>
                    <p className={styles.description}>
                        Driver ranking based on K-Means clustering algorithm
                    </p>
                </header>

                <TierListFilters
                    availableSeasons={availableSeasons}
                    selectedSeasons={selectedSeasons}
                    onSeasonsChange={setSelectedSeasons}
                    nTiers={nTiers}
                    onNTiersChange={setNTiers}
                    minRaces={minRaces}
                    onMinRacesChange={setMinRaces}
                />

                {data && !loading && (
                    <div className={styles.infoRow}>
                        <div className={styles.meta}>
                            <span>Drivers: {data.meta.total_drivers}</span>
                            <span>·</span>
                            <span>
                Seasons:{' '}
                                {data.meta.seasons
                                    ? `${data.meta.seasons[0]}–${data.meta.seasons[data.meta.seasons.length - 1]}`
                                    : 'All'}
              </span>
                            <span>·</span>
                            <span>Silhouette: {data.meta.silhouette_score}</span>
                        </div>

                        <BadgeLegend/>
                    </div>
                )}

                {loading && (
                    <div className={styles.loaderWrapper}>
                        <Loader size="lg" text="Loading tier list..."/>
                    </div>
                )}

                {error && <p className={styles.error}>{error}</p>}

                {!loading && !error && data && (
                    <div className={styles.tiers}>
                        {Object.entries(data.tiers).map(([tierName, tier]) => (
                            <TierCard
                                key={tierName}
                                name={tierName}
                                tier={tier}
                                onDriverClick={handleDriverClick}
                            />
                        ))}
                    </div>
                )}
            </div>

            <DriverModal
                driver={selectedDriver}
                isOpen={isModalOpen}
                onClose={handleCloseModal}
            />
        </div>
    );
};
