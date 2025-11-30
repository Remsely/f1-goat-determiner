import {useState} from 'react';
import {SeasonPresets} from '../SeasonPresets/SeasonPresets';
import {SeasonPicker} from '../SeasonPicker/SeasonPicker';
import {Select} from '@/components/common/Select/Select';
import styles from './TierListFilters.module.scss';

interface TierListFiltersProps {
    availableSeasons: number[];
    selectedSeasons: number[];
    onSeasonsChange: (seasons: number[]) => void;
    nTiers: number;
    onNTiersChange: (n: number) => void;
    minRaces: number;
    onMinRacesChange: (n: number) => void;
}

const TIER_OPTIONS = [
    {value: 2, label: '2'},
    {value: 3, label: '3'},
    {value: 4, label: '4'},
    {value: 5, label: '5'},
    {value: 6, label: '6'},
];

const MIN_RACES_OPTIONS = [
    {value: 1, label: '1+'},
    {value: 5, label: '5+'},
    {value: 10, label: '10+'},
    {value: 20, label: '20+'},
    {value: 50, label: '50+'},
    {value: 100, label: '100+'},
];

export const TierListFilters = ({
                                    availableSeasons,
                                    selectedSeasons,
                                    onSeasonsChange,
                                    nTiers,
                                    onNTiersChange,
                                    minRaces,
                                    onMinRacesChange,
                                }: TierListFiltersProps) => {
    const [activePreset, setActivePreset] = useState('all');
    const [showPicker, setShowPicker] = useState(false);

    const handlePresetSelect = (presetId: string, seasons: number[]) => {
        setActivePreset(presetId);
        setShowPicker(false);
        onSeasonsChange(seasons);
    };

    const handleCustomClick = () => {
        setActivePreset('custom');
        setShowPicker(true);
    };

    const handleToggleSeason = (year: number) => {
        const newSeasons = selectedSeasons.includes(year)
            ? selectedSeasons.filter((y) => y !== year)
            : [...selectedSeasons, year].sort((a, b) => a - b);
        onSeasonsChange(newSeasons);
    };

    return (
        <div className={styles.filters}>
            <div className={styles.group}>
                <label className={styles.label}>Era</label>
                <SeasonPresets
                    activePreset={activePreset}
                    availableSeasons={availableSeasons}
                    onPresetSelect={handlePresetSelect}
                    onCustomClick={handleCustomClick}
                    isCustomActive={activePreset === 'custom'}
                />
            </div>

            {showPicker && (
                <SeasonPicker
                    availableSeasons={availableSeasons}
                    selectedSeasons={selectedSeasons}
                    onToggleSeason={handleToggleSeason}
                    onSelectAll={() => onSeasonsChange([...availableSeasons])}
                    onClear={() => onSeasonsChange([])}
                />
            )}

            <div className={styles.row}>
                <Select
                    label="Number of tiers"
                    value={nTiers}
                    options={TIER_OPTIONS}
                    onChange={onNTiersChange}
                />
                <Select
                    label="Minimum races"
                    value={minRaces}
                    options={MIN_RACES_OPTIONS}
                    onChange={onMinRacesChange}
                />
            </div>
        </div>
    );
};
