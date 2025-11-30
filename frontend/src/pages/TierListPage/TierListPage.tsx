import {BackLink} from '@/components/common/BackLink/BackLink';
import {TierListFilters} from '@/components/tierList/TierListFilters/TierListFilters';
import {TierCard} from '@/components/tierList/TierCard/TierCard';
import {useTierList} from '@/hooks/useTierList';
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

    return (
        <div className={styles.page}>
            <div className="container">
                <BackLink to="/">← Back to home</BackLink>

                <header className={styles.header}>
                    <h1 className={styles.title}>Tier List</h1>
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
                    <div className={styles.meta}>
                        <span>Pilots: {data.meta.total_drivers}</span>
                        <span>
              Seasons:{' '}
                            {data.meta.seasons
                                ? `${data.meta.seasons[0]}–${data.meta.seasons[data.meta.seasons.length - 1]}`
                                : 'All'}
                        </span>
                        <span>Silhouette: {data.meta.silhouette_score}</span>
                    </div>
                )}

                {loading && <div className={styles.loading}>Loading...</div>}

                {error && <p className={styles.error}>{error}</p>}

                {!loading && !error && data && (
                    <div className={styles.tiers}>
                        {Object.entries(data.tiers).map(([tierName, tier]) => (
                            <TierCard key={tierName} name={tierName} tier={tier}/>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};
