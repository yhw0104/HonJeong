import { checkInMode } from './statusView';

describe('checkInMode', () => {
  it('SEEKING → seeking', () => expect(checkInMode('SEEKING')).toBe('seeking'));
  it('ACTIVE → dining', () => expect(checkInMode('ACTIVE')).toBe('dining'));
  it('TOGETHER → together', () => expect(checkInMode('TOGETHER')).toBe('together'));
});
