import styles from './Select.module.scss';

interface SelectOption {
  value: number | string;
  label: string;
}

interface SelectProps {
  label: string;
  value: number | string;
  options: SelectOption[];
  onChange: (value: number) => void;
}

export const Select = ({ label, value, options, onChange }: SelectProps) => {
  return (
    <div className={styles.wrapper}>
      <label className={styles.label}>{label}</label>
      <select className={styles.select} value={value} onChange={(e) => onChange(Number(e.target.value))}>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  );
};
