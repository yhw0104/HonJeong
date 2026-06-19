# 혼정 ETL — 식당 데이터 적재

백엔드 앱과 **분리된** 외부 로더. 앱은 `places`를 읽기만 하고, **채우는 일은 여기서** 한다.
(설계 배경: [`docs/07-식당데이터-전략.md`](../docs/07-식당데이터-전략.md))

## 무엇을 하나
공공데이터 **전국일반음식점표준데이터**(약 213만, CP949) →
영업/관리번호/좌표 필터 → **EPSG:5174 → WGS84 좌표변환**(pyproj, 백엔드와 동일 정의) →
`places` **멱등 upsert**(`ON CONFLICT (source, source_id)`). 초기 1회 적재·매일 동기화 모두 동일.

## 준비
1. **DB 스키마 먼저** — 백엔드를 한 번 띄워 Flyway가 `places`(V2~V4)를 만들게 한다.
   ```bash
   cd ../backend && docker compose up -d db && ./gradlew bootRun   # 테이블 생성 후 종료(Ctrl+C)
   ```
2. **CSV 다운로드** — [localdata.go.kr](https://www.localdata.go.kr/) → 전국일반음식점 **CSV**(xlsx 금지, 100만행 한계).
3. **의존성**
   ```bash
   cd etl
   python3 -m venv .venv && source .venv/bin/activate
   pip install -r requirements.txt
   ```

## 실행
```bash
python load_places.py \
  --csv ~/honjeong-data/general-restaurants.csv \
  --dsn "postgresql://honjeong:honjeong@localhost:5432/honjeong"
# 로그: 완료 — read=2,129,830 upserted=... skipped(폐업=.. 좌표결측=.. 관리번호없음=..)
```
멱등이라 **다시 돌려도 안전**(중복 없이 갱신). 헤더명이 실제 파일과 다르면 `load_places.py` 상단 `H_*` 상수만 고친다.

## 검증
```bash
docker exec -it honjeong-db psql -U honjeong -d honjeong -c "SELECT count(*) FROM places;"
# 좌표 검증: 아는 식당 위경도를 지도에서 확인 (한국 범위 위도 33~39 / 경도 124~132)
docker exec -it honjeong-db psql -U honjeong -d honjeong \
  -c "SELECT name, latitude, longitude FROM places WHERE name LIKE '%스타벅스%' LIMIT 3;"
```

## 매일 동기화 (P2)
LOCALDATA 오픈API의 **변경분**(전일까지)을 같은 스크립트 형태로 받아 upsert하고 cron으로 돌린다.
폐업분은 `business_status` 갱신으로 앱에서 자동 숨김(검색/주변이 `business_status='영업'`만 노출).

## 더 빠른 대안
213만을 더 빠르게 넣으려면 `COPY`(스테이징 테이블) 또는 PostGIS `ST_Transform`을 쓸 수 있다.
이 스크립트는 청크 단위(기본 5000행) 커밋 + `execute_values` 배치라 긴 단일 트랜잭션을 피하면서 충분히 실용적이다.
