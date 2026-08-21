import { nextSnap } from './sheetSnap';

// 실기 지적: "맨 위에서 아래로 밀면 무조건 중간에 한 번 걸려야 한다. 위로 밀 때도 마찬가지."
// 예전에는 크게·빠르게 끌면 가운데를 건너뛰었다. 그 동작이 되살아나면 여기서 잡힌다.
describe('nextSnap', () => {
  describe('아래로 밀기', () => {
    it('★맨 위에서 아래로 밀면 접히지 않고 중간에 걸린다', () => {
      expect(nextSnap('expanded', 80)).toBe('mid');
    });

    it('★세게 끌어도 중간을 건너뛰지 않는다 — 예전엔 160px을 넘으면 통과했다', () => {
      expect(nextSnap('expanded', 400)).toBe('mid');
    });

    it('중간에서 아래로 밀면 접힌다', () => {
      expect(nextSnap('mid', 80)).toBe('collapsed');
    });

    it('이미 접혀 있으면 더 내려가지 않는다', () => {
      expect(nextSnap('collapsed', 400)).toBe('collapsed');
    });
  });

  describe('위로 밀기', () => {
    it('★맨 아래에서 위로 밀면 다 펴지지 않고 중간에 걸린다', () => {
      expect(nextSnap('collapsed', -80)).toBe('mid');
    });

    it('★세게 끌어도 중간을 건너뛰지 않는다', () => {
      expect(nextSnap('collapsed', -400)).toBe('mid');
    });

    it('중간에서 위로 밀면 다 펴진다', () => {
      expect(nextSnap('mid', -80)).toBe('expanded');
    });

    it('이미 다 펴져 있으면 더 올라가지 않는다', () => {
      expect(nextSnap('expanded', -400)).toBe('expanded');
    });
  });

  describe('임계값', () => {
    it('조금만 움직이면 제자리 — 탭이나 손떨림을 단계 이동으로 읽지 않는다', () => {
      expect(nextSnap('mid', 10)).toBe('mid');
      expect(nextSnap('mid', -10)).toBe('mid');
      expect(nextSnap('mid', 0)).toBe('mid');
    });

    it('경계(50)는 아직 제자리, 넘어서면 움직인다', () => {
      expect(nextSnap('mid', 50)).toBe('mid');
      expect(nextSnap('mid', 51)).toBe('collapsed');
    });
  });
});
