import { Alert } from 'react-native';
import type { CheckIn } from './api';
import { useEndCheckIn, useCancelCheckIn } from './queries';

/**
 * 혼밥/같이먹기 종료 플로우 훅. 끝내기 탭 시 항상 확인 다이얼로그를 띄운다(실수 탭 방지).
 * '완료'는 기록에 남기고(ENDED), '취소'는 기록 없이 무효화(CANCELLED), '닫기'는 아무 것도 하지 않는다.
 * (호출부에서 SEEKING은 걸러지므로 여기 오는 건 ACTIVE/TOGETHER뿐이다.)
 */
export function usePromptEndCheckIn() {
  const end = useEndCheckIn();
  const cancel = useCancelCheckIn();

  return (checkIn: CheckIn) => {
    const together = checkIn.status === 'TOGETHER';
    Alert.alert(
      together ? '같이 먹기를 끝낼까요?' : '혼밥을 끝낼까요?',
      '완료하면 기록에 남고, 취소하면 기록되지 않아요.',
      [
        { text: '완료 (기록됨)', onPress: () => end.mutate(checkIn.checkInId) },
        { text: '취소 (기록 안 됨)', style: 'destructive', onPress: () => cancel.mutate(checkIn.checkInId) },
        { text: '닫기', style: 'cancel' },
      ],
    );
  };
}
