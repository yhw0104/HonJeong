// HonjeongMap — 카카오맵 JS SDK를 WebView로 띄우는 지도 컴포넌트.
// MapBackground(플레이스홀더)를 대체한다. 기본은 화면을 꽉 채우는 absoluteFill이라 드롭인 교체가 된다.
//
// 동작: 인라인 HTML에 카카오 JS SDK를 로드 → 지도 생성 → 'ready' 메시지로 로딩 종료.
// 상태 처리: ① 키 미설정 → 안내 화면, ② 로딩 → 스피너, ③ SDK/도메인 오류·타임아웃 → 에러 안내(+실제 메시지).
//
// 디버깅: WebView 내부의 console.error / window.onerror / 네트워크 오류를 postMessage로 끌어올려
// 화면과 Metro 터미널([HonjeongMap] 로그)에 그대로 보여준다. 카카오 도메인 오류는 여기에 원문이 찍힌다.
//
// 전제: 카카오 콘솔에 KAKAO_MAP_BASE_URL 도메인을 등록하고 카카오맵 사용을 켜야 한다(config/kakao.ts 참고).
import React, { forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react';
import { View, Text, ActivityIndicator, StyleSheet, StyleProp, ViewStyle } from 'react-native';
import { WebView, WebViewMessageEvent } from 'react-native-webview';
import { T2 } from '@/shared/theme';
import {
  KAKAO_JS_KEY,
  KAKAO_MAP_BASE_URL,
  DEFAULT_MAP_CENTER,
  DEFAULT_MAP_LEVEL,
} from '@/shared/config/kakao';

type LatLng = { lat: number; lng: number };

/** 지도에 그릴 마커 1개: 식당 위치 + 현재 혼밥러 수. */
export type MapMarkerInput = {
  placeId: number; latitude: number; longitude: number; activeCount: number;
};

/** 부모가 ref로 호출하는 명령형 핸들(줌 인/아웃·내 위치로 이동). */
export type HonjeongMapHandle = {
  zoomIn: () => void;
  zoomOut: () => void;
  recenter: () => void;
};

type Props = {
  /** 지도 중심. 기본은 연남동 부근. */
  center?: LatLng;
  /** 확대 레벨(작을수록 확대). 기본 4. */
  level?: number;
  /** 컨테이너 스타일. 미지정 시 화면을 꽉 채운다(MapBackground 드롭인). */
  style?: StyleProp<ViewStyle>;
  /** 지도에 표시할 마커 목록(실좌표 핀 + 혼밥러수 라벨). */
  markers?: MapMarkerInput[];
  /** 마커 탭 시 호출(식당 상세 이동 등). */
  onMarkerPress?: (placeId: number) => void;
  /** 내 위치(파란 점). null/미지정이면 표시 안 함. */
  myLocation?: LatLng | null;
};

/** 카카오 JS SDK를 로드해 지도를 그리는 HTML 문서를 만든다. */
function buildHtml(appKey: string, center: LatLng, level: number): string {
  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />
  <style>html,body,#map{margin:0;padding:0;width:100%;height:100%;overflow:hidden;}</style>
</head>
<body>
  <div id="map"></div>
  <script>
    var done = false;
    function post(m){ if (window.ReactNativeWebView) window.ReactNativeWebView.postMessage(String(m)); }
    // WebView 내부 오류를 RN으로 끌어올린다(카카오 도메인/키 오류 원문 포함).
    window.onerror = function(msg){ post('jserr:' + msg); };
    var _ce = console.error;
    console.error = function(){ try { post('console:' + Array.prototype.slice.call(arguments).join(' ')); } catch(e){} _ce.apply(console, arguments); };
    // SDK가 끝내 준비되지 않으면(키·도메인 문제) 무한 스피너를 막는다.
    setTimeout(function(){ if (!done) post('error:timeout'); }, 8000);
  </script>
  <script
    src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&autoload=false"
    onerror="post('error:script-load-failed')"></script>
  <script>
    if (typeof kakao === 'undefined') {
      post('error:kakao-undefined');
    } else {
      try {
        kakao.maps.load(function () {
          var map = new kakao.maps.Map(document.getElementById('map'), {
            center: new kakao.maps.LatLng(${center.lat}, ${center.lng}),
            level: ${level}
          });
          window.__map = map;
          // 내 위치(파란 점) 렌더 함수. lat/lng가 null이면 점을 제거한다.
          window.__myLoc = null;
          window.__renderMyLocation = function(lat, lng){
            if (window.__myLoc) { window.__myLoc.setMap(null); window.__myLoc = null; }
            if (lat == null || lng == null) return;
            window.__myLoc = new kakao.maps.CustomOverlay({
              position: new kakao.maps.LatLng(lat, lng),
              content: '<div style="width:18px;height:18px;border-radius:9px;background:#2D7DF6;border:3px solid #fff;box-shadow:0 0 0 4px rgba(45,125,246,0.25);"></div>',
              zIndex: 10
            });
            window.__myLoc.setMap(map);
          };
          // RN에서 주입할 마커 렌더 함수. 기존 마커를 지우고 새 목록으로 다시 그린다.
          window.__markers = [];
          window.__renderMarkers = function(list){
            (window.__markers || []).forEach(function(o){ o.setMap(null); });
            window.__markers = [];
            (list || []).forEach(function(it){
              var pos = new kakao.maps.LatLng(it.latitude, it.longitude);
              // 알약(타원) 마커: 동그라미 + 옆에 혼밥러 수. 클릭하면 식당 상세로.
              var el = document.createElement('div');
              el.style.cssText = 'display:flex;align-items:center;gap:5px;background:#fff;border:2px solid #FF5A36;border-radius:999px;padding:3px 9px 3px 5px;box-shadow:0 2px 6px rgba(0,0,0,0.25);cursor:pointer;';
              el.innerHTML = '<div style="width:14px;height:14px;border-radius:7px;background:#FF5A36;"></div>'
                + '<span style="color:#FF5A36;font-weight:800;font-size:12px;line-height:1;">' + it.activeCount + '</span>';
              el.addEventListener('click', function(){ post('marker:' + it.placeId); });
              var overlay = new kakao.maps.CustomOverlay({ position: pos, content: el, clickable: true });
              overlay.setMap(map);
              window.__markers.push(overlay);
            });
          };
          window.__renderMarkers([]);
          done = true;
          post('ready');
        });
      } catch (e) {
        post('jserr:' + (e && e.message ? e.message : e));
      }
    }
  </script>
</body>
</html>`;
}

export const HonjeongMap = forwardRef<HonjeongMapHandle, Props>(function HonjeongMap({
  center = DEFAULT_MAP_CENTER,
  level = DEFAULT_MAP_LEVEL,
  style,
  markers,
  onMarkerPress,
  myLocation,
}: Props, ref) {
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [detail, setDetail] = useState<string>('');
  const html = useMemo(() => buildHtml(KAKAO_JS_KEY, center, level), [center, level]);
  const webRef = useRef<WebView>(null);
  const run = (js: string) => webRef.current?.injectJavaScript(js + ' true;');

  // 부모가 ref로 호출하는 줌/내위치 명령(WebView에 JS 주입).
  useImperativeHandle(ref, () => ({
    zoomIn: () => run('window.__map && window.__map.setLevel(window.__map.getLevel() - 1);'),
    zoomOut: () => run('window.__map && window.__map.setLevel(window.__map.getLevel() + 1);'),
    recenter: () => {
      if (myLocation) {
        run(`window.__map && window.__map.setCenter(new kakao.maps.LatLng(${myLocation.lat}, ${myLocation.lng}));`);
      }
    },
  }));

  // 지도가 준비되거나 markers가 바뀌면 마커 목록을 주입한다.
  useEffect(() => {
    if (status !== 'ready') return;
    run(`window.__renderMarkers && window.__renderMarkers(${JSON.stringify(markers ?? [])});`);
  }, [markers, status]);

  // 내 위치(파란 점) 주입. 좌표가 없으면 점 제거.
  useEffect(() => {
    if (status !== 'ready') return;
    if (myLocation) run(`window.__renderMyLocation && window.__renderMyLocation(${myLocation.lat}, ${myLocation.lng});`);
    else run('window.__renderMyLocation && window.__renderMyLocation(null, null);');
  }, [myLocation, status]);

  // 키 미설정: WebView를 띄우지 않고 안내 화면을 보여준다.
  if (!KAKAO_JS_KEY) {
    return (
      <View style={[styles.fill, styles.center, style]}>
        <Text style={styles.guideTitle}>카카오 JS 키가 필요합니다</Text>
        <Text style={styles.guideBody}>
          src/shared/config/kakao.ts 의{'\n'}KAKAO_JS_KEY 를 채워주세요.
        </Text>
      </View>
    );
  }

  const onMessage = (e: WebViewMessageEvent) => {
    const msg = e.nativeEvent.data;
    console.log('[HonjeongMap]', msg); // Metro 터미널에서도 보이게
    if (msg === 'ready') {
      setStatus('ready');
    } else if (msg.startsWith('error')) {
      setStatus('error');
      setDetail((d) => d || msg);
    } else if (msg.startsWith('marker:')) {
      const placeId = Number(msg.slice('marker:'.length));
      if (!Number.isNaN(placeId)) onMarkerPress?.(placeId);
    } else if (msg.startsWith('console:') || msg.startsWith('jserr:')) {
      // 카카오가 도메인/키 오류를 console.error로 찍는다 → 가장 마지막 원문을 보존.
      setDetail(msg);
    }
  };

  return (
    <View style={[styles.fill, style]}>
      <WebView
        ref={webRef}
        style={styles.fill}
        originWhitelist={['*']}
        source={{ html, baseUrl: KAKAO_MAP_BASE_URL }}
        javaScriptEnabled
        domStorageEnabled
        onMessage={onMessage}
        onError={(e) => {
          console.log('[HonjeongMap] webview onError', e.nativeEvent);
          setStatus('error');
          setDetail((d) => d || `webview: ${e.nativeEvent.description}`);
        }}
        onHttpError={(e) => {
          console.log('[HonjeongMap] webview httpError', e.nativeEvent);
          setDetail((d) => d || `http ${e.nativeEvent.statusCode}: ${e.nativeEvent.url}`);
        }}
        scrollEnabled={false}
      />

      {status === 'loading' && (
        <View style={[styles.fill, styles.center, styles.overlay]} pointerEvents="none">
          <ActivityIndicator color={T2.brand} />
        </View>
      )}

      {status === 'error' && (
        <View style={[styles.fill, styles.center, styles.overlay]}>
          <Text style={styles.guideTitle}>지도를 불러오지 못했습니다</Text>
          <Text style={styles.guideBody}>
            카카오 콘솔에서 ① JS 키, ② 카카오맵 "사용" ON,{'\n'}③ Web 도메인({KAKAO_MAP_BASE_URL}) 등록을 확인해주세요.
          </Text>
          {!!detail && <Text style={styles.detail}>{detail}</Text>}
        </View>
      )}
    </View>
  );
});

const styles = StyleSheet.create({
  fill: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 },
  center: { alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24 },
  overlay: { backgroundColor: T2.mapBg },
  guideTitle: { color: T2.text, fontSize: 15, fontWeight: '700', marginBottom: 8 },
  guideBody: { color: T2.textSub, fontSize: 13, lineHeight: 20, textAlign: 'center' },
  detail: {
    marginTop: 14,
    color: T2.textMute,
    fontSize: 11,
    lineHeight: 16,
    textAlign: 'center',
  },
});
