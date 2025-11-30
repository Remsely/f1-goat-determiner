import {useEffect, useState, useCallback} from 'react';
import {Link} from 'react-router-dom';
import {tierListApi} from '@/api/tierList';
import type {TierListResponse} from '@/api/types';
import styles from './TierListPage.module.scss';

export const TierListPage = () => {
    const [data, setData] = useState<TierListResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [availableSeasons, setAvailableSeasons] = useState<number[]>([]);
    const [selectedSeasons, setSelectedSeasons] = useState<number[]>([]);
    const [activePreset, setActivePreset] = useState<string>('all');
    const [nTiers, setNTiers] = useState(4);
    const [minRaces, setMinRaces] = useState(10);
    const [showSeasonPicker, setShowSeasonPicker] = useState(false);

    useEffect(() => {
        tierListApi.getSeasons().then((res) => {
            setAvailableSeasons(res.seasons);
        });
    }, []);

    const loadTierList = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const result = await tierListApi.getTierList({
                seasons: selectedSeasons.length > 0 ? selectedSeasons : undefined,
                nTiers,
                minRaces,
            });
            setData(result);
        } catch (err) {
            setError('Ошибка загрузки данных');
            console.error(err);
        } finally {
            setLoading(false);
        }
    }, [selectedSeasons, nTiers, minRaces]);

    useEffect(() => {
        loadTierList();
    }, [loadTierList]);

    const applyPreset = (preset: string) => {
        const currentYear = new Date().getFullYear();

        if (preset === 'custom') {
            setShowSeasonPicker(true);
            setActivePreset('custom');
            return;
        }

        setShowSeasonPicker(false);
        setActivePreset(preset);

        switch (preset) {
            case 'all':
                setSelectedSeasons([]);
                break;
            case 'modern':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 2014));
                break;
            case 'last5':
                setSelectedSeasons(availableSeasons.filter((y) => y > currentYear - 5));
                break;
            case 'last10':
                setSelectedSeasons(availableSeasons.filter((y) => y > currentYear - 10));
                break;
            case '2020s':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 2020));
                break;
            case '2010s':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 2010 && y < 2020));
                break;
            case '2000s':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 2000 && y < 2010));
                break;
            case '1990s':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 1990 && y < 2000));
                break;
            case '1980s':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 1980 && y < 1990));
                break;
            case '1970s':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 1970 && y < 1980));
                break;
            case '1960s':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 1960 && y < 1970));
                break;
            case '1950s':
                setSelectedSeasons(availableSeasons.filter((y) => y >= 1950 && y < 1960));
                break;
        }
    };

    const toggleSeason = (year: number) => {
        setSelectedSeasons((prev) =>
            prev.includes(year)
                ? prev.filter((y) => y !== year)
                : [...prev, year].sort((a, b) => a - b)
        );
    };

    const selectAllSeasons = () => setSelectedSeasons([...availableSeasons]);
    const clearAllSeasons = () => setSelectedSeasons([]);

    const isPresetActive = (preset: string): boolean => {
        return activePreset === preset;
    };

    return (
        <div className={styles.page}>
            <div className="container">
                <Link to="/" className={styles.backLink}>← На главную</Link>

                <div className={styles.header}>
                    <h1 className={styles.title}>Tier List</h1>
                </div>

                {/* Фильтры */}
                <div className={styles.filters}>
                    <div className={styles.filterGroup}>
                        <label className={styles.filterLabel}>Эпоха</label>
                        <div className={styles.presets}>
                            <button
                                className={`${styles.preset} ${isPresetActive('all') ? styles.active : ''}`}
                                onClick={() => applyPreset('all')}
                            >
                                Все сезоны
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('modern') ? styles.active : ''}`}
                                onClick={() => applyPreset('modern')}
                            >
                                Гибриды (2014+)
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('last5') ? styles.active : ''}`}
                                onClick={() => applyPreset('last5')}
                            >
                                Последние 5 лет
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('last10') ? styles.active : ''}`}
                                onClick={() => applyPreset('last10')}
                            >
                                Последние 10 лет
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('2020s') ? styles.active : ''}`}
                                onClick={() => applyPreset('2020s')}
                            >
                                2020-е
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('2010s') ? styles.active : ''}`}
                                onClick={() => applyPreset('2010s')}
                            >
                                2010-е
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('2000s') ? styles.active : ''}`}
                                onClick={() => applyPreset('2000s')}
                            >
                                2000-е
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('1990s') ? styles.active : ''}`}
                                onClick={() => applyPreset('1990s')}
                            >
                                1990-е
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('1980s') ? styles.active : ''}`}
                                onClick={() => applyPreset('1980s')}
                            >
                                1980-е
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('1970s') ? styles.active : ''}`}
                                onClick={() => applyPreset('1970s')}
                            >
                                1970-е
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('1960s') ? styles.active : ''}`}
                                onClick={() => applyPreset('1960s')}
                            >
                                1960-е
                            </button>
                            <button
                                className={`${styles.preset} ${isPresetActive('1950s') ? styles.active : ''}`}
                                onClick={() => applyPreset('1950s')}
                            >
                                1950-е
                            </button>
                            <button
                                className={`${styles.preset} ${styles.customPreset} ${isPresetActive('custom') ? styles.active : ''}`}
                                onClick={() => applyPreset('custom')}
                            >
                                Выбрать сезоны...
                            </button>
                        </div>
                    </div>

                    {/* Мультиселектор сезонов */}
                    {showSeasonPicker && (
                        <div className={styles.seasonPicker}>
                            <div className={styles.seasonPickerHeader}>
                                <span>Выберите сезоны:</span>
                                <div className={styles.seasonPickerActions}>
                                    <button onClick={selectAllSeasons}>Выбрать все</button>
                                    <button onClick={clearAllSeasons}>Сбросить</button>
                                </div>
                            </div>
                            <div className={styles.seasonGrid}>
                                {availableSeasons.map((year) => (
                                    <button
                                        key={year}
                                        className={`${styles.seasonChip} ${
                                            selectedSeasons.includes(year) ? styles.selected : ''
                                        }`}
                                        onClick={() => toggleSeason(year)}
                                    >
                                        {year}
                                    </button>
                                ))}
                            </div>
                            {selectedSeasons.length > 0 && (
                                <div className={styles.selectedCount}>
                                    Выбрано: {selectedSeasons.length} сезонов
                                </div>
                            )}
                        </div>
                    )}

                    <div className={styles.filterRow}>
                        <div className={styles.filterGroup}>
                            <label className={styles.filterLabel}>Количество тиров</label>
                            <select
                                className={styles.select}
                                value={nTiers}
                                onChange={(e) => setNTiers(Number(e.target.value))}
                            >
                                <option value={2}>2</option>
                                <option value={3}>3</option>
                                <option value={4}>4</option>
                                <option value={5}>5</option>
                                <option value={6}>6</option>
                            </select>
                        </div>

                        <div className={styles.filterGroup}>
                            <label className={styles.filterLabel}>Минимум гонок</label>
                            <select
                                className={styles.select}
                                value={minRaces}
                                onChange={(e) => setMinRaces(Number(e.target.value))}
                            >
                                <option value={1}>1+</option>
                                <option value={5}>5+</option>
                                <option value={10}>10+</option>
                                <option value={20}>20+</option>
                                <option value={50}>50+</option>
                                <option value={100}>100+</option>
                            </select>
                        </div>
                    </div>
                </div>

                {/* Мета-информация */}
                {data && !loading && (
                    <div className={styles.meta}>
                        <span>Пилотов: {data.meta.total_drivers}</span>
                        <span>
                            Сезоны:{' '}
                            {data.meta.seasons
                                ? `${data.meta.seasons[0]}–${data.meta.seasons[data.meta.seasons.length - 1]}`
                                : 'Все'}
                        </span>
                        <span>Силуэт: {data.meta.silhouette_score}</span>
                    </div>
                )}

                {/* Контент */}
                {loading && <div className={styles.loading}>Загрузка...</div>}

                {error && <p className={styles.error}>{error}</p>}

                {!loading && !error && data && (
                    <div className={styles.tiers}>
                        {Object.entries(data.tiers).map(([tierName, tier]) => (
                            <div key={tierName} className={styles.tier} data-tier={tierName}>
                                <div className={styles.tierHeader}>
                                    <div className={styles.tierInfo}>
                                        <div className={styles.tierLabel}>{tierName}</div>
                                        <div>
                                            <div className={styles.tierName}>
                                                {getTierDescription(tierName)}
                                            </div>
                                            <div className={styles.tierStats}>
                                                <span>Ср. побед: {tier.avg_win_rate}%</span>
                                                <span> · </span>
                                                <span>Ср.  финиш: {tier.avg_finish?.toFixed(1) || 'N/A'}</span>
                                            </div>
                                        </div>
                                    </div>
                                    <div className={styles.tierCount}>{tier.count} пилотов</div>
                                </div>

                                <div className={styles.drivers}>
                                    {tier.drivers.map((driver) => (
                                        <div key={driver.id} className={styles.driver}>
                                            <div className={styles.driverInfo}>
                                                <span className={styles.driverName}>{driver.name}</span>
                                                <span className={styles.driverNationality}>
                                                    {driver.nationality}
                                                </span>
                                            </div>
                                            <div className={styles.driverStats}>
                                                <div className={styles.driverMainStats}>
                                                    {driver.stats.titles > 0 && (
                                                        <span className={styles.statBadge} data-type="titles">
                                                            {driver.stats.titles} 🏆
                                                        </span>
                                                    )}
                                                    {driver.stats.wins > 0 && (
                                                        <span className={styles.statBadge} data-type="wins">
                                                            {driver.stats.wins} W
                                                        </span>
                                                    )}
                                                    {driver.stats.podiums > 0 && (
                                                        <span className={styles.statBadge} data-type="podiums">
                                                            {driver.stats.podiums} P
                                                        </span>
                                                    )}
                                                    {driver.stats.wins === 0 && driver.stats.podiums === 0 && (
                                                        <span className={styles.statBadge} data-type="none">
                                                            —
                                                        </span>
                                                    )}
                                                </div>
                                                <div className={styles.driverRaces}>
                                                    {driver.stats.races} races · {driver.stats.win_rate.toFixed(1)}%
                                                    wins
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

function getTierDescription(tier: string): string {
    const descriptions: Record<string, string> = {
        S: 'Легенды',
        A: 'Звёзды',
        B: 'Сильные пилоты',
        C: 'Середняки',
        D: 'Нераскрывшиеся',
        F: 'Аутсайдеры',
    };
    return descriptions[tier] || tier;
}
