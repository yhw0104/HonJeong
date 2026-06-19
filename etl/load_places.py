#!/usr/bin/env python3
"""
혼정 — 공공데이터(전국일반음식점표준데이터) → places 마스터 적재 ETL.

백엔드 앱과 분리된 외부 로더. 앱은 places를 '읽기만' 하고, 채우는 일은 이 스크립트가 한다.
초기 전국 1회 적재와 매일 변경분 동기화에 모두 쓸 수 있다(둘 다 멱등 upsert).

동작:
  CSV(CP949) 스트리밍 → 영업/관리번호/좌표 필터 → EPSG:5174→WGS84 좌표변환(pyproj)
  → places 멱등 upsert(ON CONFLICT (source, source_id) DO UPDATE).

사용:
  pip install -r requirements.txt
  python load_places.py --csv ~/honjeong-data/general-restaurants.csv \
      --dsn "postgresql://honjeong:honjeong@localhost:5432/honjeong"
  # 또는 환경변수 DATABASE_URL 사용

참고: 좌표계는 EPSG:2097이 아니라 5174. towgs84 7파라미터 누락 시 ~400m 오차.
      (백엔드 CoordinateConverter와 동일한 proj 정의를 사용해 결과가 일치한다.)
"""
import argparse
import csv
import os
import sys

import psycopg2
from psycopg2.extras import execute_values
from pyproj import CRS, Transformer

# 공공데이터 EPSG:5174 → WGS84(4326). 백엔드 proj4j와 동일한 정의(towgs84 7파라미터 포함).
EPSG_5174 = (
    "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 "
    "+ellps=bessel +units=m +no_defs "
    "+towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43"
)
_TRANSFORMER = Transformer.from_crs(CRS.from_proj4(EPSG_5174), CRS.from_epsg(4326), always_xy=True)

# 표준데이터 CSV 헤더 → 우리 컬럼. 실제 다운로드 헤더가 다르면 여기만 고치면 된다.
H_MGMT = "관리번호"
H_NAME = "사업장명"
H_CATEGORY = "위생업태명"
H_ADDRESS = "지번주소"
H_ROAD = "도로명주소"
H_PHONE = "전화번호"
H_STATUS = "영업상태명"
H_X = "좌표정보(X)"
H_Y = "좌표정보(Y)"

UPSERT_SQL = """
INSERT INTO {table} (source, source_id, name, category, address, road_address,
                    latitude, longitude, phone, business_status, created_at, updated_at)
VALUES %s
ON CONFLICT (source, source_id) DO UPDATE SET
    name = EXCLUDED.name, category = EXCLUDED.category, address = EXCLUDED.address,
    road_address = EXCLUDED.road_address, latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude, phone = EXCLUDED.phone,
    business_status = EXCLUDED.business_status, updated_at = now()
"""
UPSERT_TEMPLATE = "('PUBLIC_DATA',%s,%s,%s,%s,%s,%s,%s,%s,'영업',now(),now())"


def _blank(s):
    return s is None or s.strip() == ""


def _is_open(status):
    return status is not None and ("영업" in status or "정상" in status)


def to_wgs84(x_raw, y_raw):
    """TM(5174) → (lat, lng). 변환 불가/범위밖이면 None."""
    if _blank(x_raw) or _blank(y_raw):
        return None
    try:
        x, y = float(x_raw), float(y_raw)
    except ValueError:
        return None
    if x <= 0 or y <= 0:
        return None
    lng, lat = _TRANSFORMER.transform(x, y)  # always_xy → (경도, 위도)
    if lat != lat or lng != lng:  # NaN
        return None
    return lat, lng


def iter_rows(csv_path, charset):
    with open(csv_path, encoding=charset, newline="") as f:
        for row in csv.DictReader(f):
            yield row


def load(csv_path, charset, dsn, batch_size, table="places"):
    read = upserted = skipped_closed = skipped_nocoord = skipped_noid = 0
    batch = []
    # table은 운영자가 주는 식별자(사용자 입력 아님). 화이트리스트로 한 번 더 방어.
    if not table.replace("_", "").isalnum():
        raise ValueError(f"잘못된 테이블명: {table}")
    upsert_sql = UPSERT_SQL.format(table=table)
    conn = psycopg2.connect(dsn)
    conn.autocommit = False
    try:
        with conn.cursor() as cur:
            def flush():
                nonlocal upserted
                if not batch:
                    return
                execute_values(cur, upsert_sql, batch, template=UPSERT_TEMPLATE, page_size=batch_size)
                conn.commit()  # 청크 단위 커밋 — 긴 단일 트랜잭션 회피
                upserted += len(batch)
                batch.clear()

            for row in iter_rows(csv_path, charset):
                read += 1
                if not _is_open(row.get(H_STATUS)):
                    skipped_closed += 1
                elif _blank(row.get(H_MGMT)):
                    skipped_noid += 1
                else:
                    ll = to_wgs84(row.get(H_X), row.get(H_Y))
                    if ll is None:
                        skipped_nocoord += 1
                    else:
                        lat, lng = ll
                        batch.append((
                            row[H_MGMT].strip(),
                            (row.get(H_NAME) or "").strip(),
                            (row.get(H_CATEGORY) or "").strip() or None,
                            (row.get(H_ADDRESS) or "").strip() or None,
                            (row.get(H_ROAD) or "").strip() or None,
                            lat, lng,
                            (row.get(H_PHONE) or "").strip() or None,
                        ))
                        if len(batch) >= batch_size:
                            flush()
                if read % 100_000 == 0:
                    print(f"  ...읽음 {read:,} (적재 {upserted:,})", file=sys.stderr)
            flush()
    finally:
        conn.close()
    return read, upserted, skipped_closed, skipped_nocoord, skipped_noid


def main():
    ap = argparse.ArgumentParser(description="공공데이터 음식점 → places 멱등 적재")
    ap.add_argument("--csv", required=True, help="전국일반음식점 CSV 경로")
    ap.add_argument("--charset", default="cp949", help="CSV 인코딩(기본 cp949)")
    ap.add_argument("--dsn", default=os.environ.get("DATABASE_URL"),
                    help="postgresql://user:pass@host:port/db (또는 env DATABASE_URL)")
    ap.add_argument("--batch-size", type=int, default=5000)
    ap.add_argument("--table", default="places",
                    help="적재 대상 테이블(기본 places). 검증용 스테이징은 places_staging 등)")
    args = ap.parse_args()
    if not args.dsn:
        ap.error("--dsn 또는 환경변수 DATABASE_URL 필요")

    print(f"적재 시작: {args.csv} ({args.charset}) → {args.table}", file=sys.stderr)
    read, upserted, sc, snc, sni = load(args.csv, args.charset, args.dsn, args.batch_size, args.table)
    print(
        f"완료 — read={read:,} upserted={upserted:,} "
        f"skipped(폐업={sc:,} 좌표결측={snc:,} 관리번호없음={sni:,})",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
