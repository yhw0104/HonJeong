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

/** 지도에 그릴 마커 1개: 식당 위치·이름 + 현재 혼밥러 수 + 모집중 인원. */
export type MapMarkerInput = {
  placeId: number; name: string; latitude: number; longitude: number; activeCount: number; seekingCount: number;
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
  /** 사용자가 지도를 드래그로 이동한 뒤 새 지도 중심. 재검색 판정용. */
  onCenterChange?: (center: LatLng) => void;
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
          // 사용자가 지도를 드래그해 끝냈을 때만 새 중심을 RN에 보고한다(프로그램적 setCenter는 dragend 미발화 → 피드백 루프 없음).
          kakao.maps.event.addListener(map, 'dragend', function(){
            var c = map.getCenter();
            post('center:' + c.getLat() + ',' + c.getLng());
          });
          // 내 위치(파란 점) 렌더 함수. lat/lng가 null이면 점을 제거한다.
          window.__myLoc = null;
          window.__renderMyLocation = function(lat, lng){
            if (window.__myLoc) { window.__myLoc.setMap(null); window.__myLoc = null; }
            if (lat == null || lng == null) return;
            window.__myLoc = new kakao.maps.CustomOverlay({
              position: new kakao.maps.LatLng(lat, lng),
              content: '<div style="width:18px;height:18px;border-radius:50%;background:#2D7DF6;border:3px solid #fff;box-shadow:0 0 0 4px rgba(45,125,246,0.25);"></div>',
              zIndex: 10
            });
            window.__myLoc.setMap(map);
          };
          // RN에서 주입할 마커 렌더 함수. 기존 마커를 지우고 새 목록으로 다시 그린다.
          window.__markers = [];
          window.__labels = [];
          var LABEL_MAX_LEVEL = 4; // 이 레벨 이하(확대)에서만 이름 표시 — 줌 아웃되면 라벨이 겹치지 않게 숨긴다.
          window.__updateLabels = function(){
            var show = map.getLevel() <= LABEL_MAX_LEVEL;
            (window.__labels || []).forEach(function(l){ l.style.display = show ? '' : 'none'; });
          };
          kakao.maps.event.addListener(map, 'zoom_changed', window.__updateLabels);
          // RN에서 주입할 마커 렌더 함수. 기존 마커를 지우고 새 목록으로 다시 그린다.
          window.__renderMarkers = function(list){
            (window.__markers || []).forEach(function(o){ o.setMap(null); });
            window.__markers = [];
            window.__labels = [];
            list = list || [];
            // 위경도가 완전히 같은 식당들(같은 건물 등)은 겹치므로, 같은 좌표 그룹을 작은 원형으로 살짝 흩뿌린다(모두 보이고 클릭되게).
            var byCoord = {}, seen = {};
            list.forEach(function(it){
              var key = Number(it.latitude).toFixed(6) + ',' + Number(it.longitude).toFixed(6);
              (byCoord[key] = byCoord[key] || []).push(it);
            });
            list.forEach(function(it){
              var key = Number(it.latitude).toFixed(6) + ',' + Number(it.longitude).toFixed(6);
              var group = byCoord[key];
              var lat = Number(it.latitude), lng = Number(it.longitude);
              if (group.length > 1) {
                var idx = seen[key] == null ? 0 : seen[key] + 1; seen[key] = idx;
                var ang = (2 * Math.PI * idx) / group.length;
                var R = 0.00006; // ≈6.6m 반경으로 방사
                lat += R * Math.cos(ang);
                lng += R * Math.sin(ang);
              }
              var pos = new kakao.maps.LatLng(lat, lng);
              var seeking = it.seekingCount || 0;
              // 세로 스택: [마커] 위 + [식당 이름] 아래. 클릭하면 식당 상세로.
              var el = document.createElement('div');
              el.style.cssText = 'display:flex;flex-direction:column;align-items:center;gap:3px;cursor:pointer;';
              var pin = document.createElement('div');
              if (seeking > 0) {
                // 모집중 있음: 주황 알약 + 인원 수(강조).
                pin.style.cssText = 'display:flex;align-items:center;gap:5px;background:#fff;border:2px solid #FF5A36;border-radius:999px;padding:3px 9px 3px 5px;box-shadow:0 2px 6px rgba(0,0,0,0.25);';
                pin.innerHTML = '<div style="width:14px;height:14px;border-radius:50%;background:#FF5A36;"></div>'
                  + '<span style="color:#FF5A36;font-weight:800;font-size:12px;line-height:1;">' + seeking + '</span>';
              } else {
                // 모집중 없음: 주황 점(위치만 표시).
                pin.style.cssText = 'width:14px;height:14px;border-radius:50%;background:#FF5A36;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,0.3);';
              }
              var label = document.createElement('div');
              label.textContent = it.name || ''; // textContent = XSS 안전(식당명 그대로)
              // 배경 박스 없이 — 흰색 외곽선(text-shadow)으로 지도 위에서 읽히게.
              label.style.cssText = 'max-width:110px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:11px;font-weight:800;color:#333;text-shadow:0 0 3px #fff,0 0 3px #fff,0 0 2px #fff;';
              el.appendChild(pin);
              el.appendChild(label);
              window.__labels.push(label);
              el.addEventListener('click', function(){ post('marker:' + it.placeId); });
              var overlay = new kakao.maps.CustomOverlay({ position: pos, content: el, clickable: true });
              overlay.setMap(map);
              window.__markers.push(overlay);
            });
            window.__updateLabels(); // 현재 줌 레벨에 맞춰 이름 표시 여부 초기화
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
  onCenterChange,
  myLocation,
}: Props, ref) {
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [detail, setDetail] = useState<string>('');
  // HTML은 최초 center/level로 1회만 생성한다 — center prop이 바뀔 때마다 문서를 다시 만들면
  // WebView가 통째로 리로드되어 지도가 깜빡인다. 이후 center 변경은 아래 setCenter 주입으로 반영.
  const initialRef = useRef({ center, level });
  const html = useMemo(() => buildHtml(KAKAO_JS_KEY, initialRef.current.center, initialRef.current.level), []);
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

  // center prop 변경(GPS 취득 등)은 리로드 없이 지도 이동으로 반영한다.
  useEffect(() => {
    if (status !== 'ready') return;
    run(`window.__map && window.__map.setCenter(new kakao.maps.LatLng(${center.lat}, ${center.lng}));`);
  }, [center.lat, center.lng, status]);

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
    } else if (msg.startsWith('center:')) {
      const [lat, lng] = msg.slice('center:'.length).split(',').map(Number);
      if (!Number.isNaN(lat) && !Number.isNaN(lng)) onCenterChange?.({ lat, lng });
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
