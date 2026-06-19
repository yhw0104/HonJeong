-- places: 공공데이터 마스터 컬럼 추가 (식당 데이터는 비어 있어 데이터 이관 없음).
-- external_id/homepage_url 제거는 호출부 정리 후 Task 8(V3)에서 수행한다.
ALTER TABLE places ALTER COLUMN external_id DROP NOT NULL;     -- 적재 행은 external_id 없음
ALTER TABLE places ADD COLUMN source          VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC_DATA';
ALTER TABLE places ADD COLUMN source_id       VARCHAR(64);
ALTER TABLE places ADD COLUMN road_address    VARCHAR(300);
ALTER TABLE places ADD COLUMN business_status VARCHAR(20);
ALTER TABLE places ALTER COLUMN address TYPE VARCHAR(300);
ALTER TABLE places ALTER COLUMN phone   TYPE VARCHAR(40);
ALTER TABLE places ADD CONSTRAINT uq_places_source UNIQUE (source, source_id);

-- 이름 부분일치 검색용 trigram 인덱스
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_places_name_trgm ON places USING gin (name gin_trgm_ops);
-- idx_places_lat_lng (위경도 바운딩박스)는 V1에 이미 존재
