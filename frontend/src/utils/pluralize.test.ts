import { pluralize } from './pluralize';

describe('pluralize', () => {
  it('returns singular when count is 1', () => {
    expect(pluralize(1, 'driver', 'drivers')).toBe('driver');
  });

  it('returns plural when count is 0', () => {
    expect(pluralize(0, 'driver', 'drivers')).toBe('drivers');
  });

  it('returns plural when count is greater than 1', () => {
    expect(pluralize(5, 'driver', 'drivers')).toBe('drivers');
  });
});
