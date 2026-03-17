import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { Select } from './Select';

describe('Select', () => {
  const options = [
    { value: 2, label: 'Two' },
    { value: 4, label: 'Four' },
    { value: 6, label: 'Six' },
  ];

  it('renders label and options', () => {
    render(<Select label="Count" value={4} options={options} onChange={vi.fn()} />);

    expect(screen.getByText('Count')).toBeInTheDocument();
    expect(screen.getByRole('combobox')).toHaveValue('4');
    expect(screen.getAllByRole('option')).toHaveLength(3);
  });

  it('calls onChange with numeric value on selection', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(<Select label="Count" value={4} options={options} onChange={onChange} />);

    await user.selectOptions(screen.getByRole('combobox'), '6');

    expect(onChange).toHaveBeenCalledWith(6);
  });
});
