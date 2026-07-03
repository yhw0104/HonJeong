import { Alert } from 'react-native';
import type { CheckIn } from './api';
import { decideEndAction } from './endFlow';
import { useEndCheckIn, useCancelCheckIn } from './queries';

/**
 * 혼밥 종료 플로우 훅. 30분 미만 종료면 "정말 혼밥하셨나요?" Alert로 확인해
 * "아니에요"면 취소(CANCELLED), 그 외엔 종료(ENDED)한다. TOGETHER는 바로 종료.
 */
export function usePromptEndCheckIn() {
  const end = useEndCheckIn();
  const cancel = useCancelCheckIn();

  return (checkIn: CheckIn) => {
    if (decideEndAction(checkIn, Date.now()) === 'end') {
      end.mutate(checkIn.checkInId);
      return;
    }
    Alert.alert('정말 혼밥하셨나요?', '30분이 안 돼서 확인해요.', [
      { text: '혼밥했어요', onPress: () => end.mutate(checkIn.checkInId) },
      { text: '아니에요', style: 'destructive', onPress: () => cancel.mutate(checkIn.checkInId) },
    ]);
  };
}
