-- 검색 predicate가 LOWER(name)이므로 functional trigram 인덱스로 교체
DROP INDEX IF EXISTS idx_places_name_trgm;
CREATE INDEX idx_places_lower_name_trgm ON places USING gin (lower(name) gin_trgm_ops);
