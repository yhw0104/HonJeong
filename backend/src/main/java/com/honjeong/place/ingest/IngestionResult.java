package com.honjeong.place.ingest;

public record IngestionResult(int read, int upserted, int skippedClosed, int skippedNoCoord) {}
