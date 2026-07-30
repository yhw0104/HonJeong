package com.honjeong.file.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 파일을 저장하고 접근 URL을 돌려주는 파일 저장소 추상화 인터페이스.
 *
 * <p>사용처: FileService(저장 위임) — 구현체는 LocalFileStorage(mock-first 개발용, 운영 시 S3 구현으로 교체 예정).
 * <p>파일 저장의 책임을 정의하는 인터페이스. 업로드된 파일을 저장하고 접근 가능한 URL을 돌려준다.
 *
 * <p>실제 저장 위치는 환경에 따라 구현이 갈린다 — 개발용 {@link LocalFileStorage}는 로컬 디렉터리에 저장하고,
 * 운영용 구현(S3 등 오브젝트 스토리지)은 클라우드에 저장한다. 인터페이스로 추상화해 {@code FileService}는 구현 교체에
 * 영향받지 않는다(SMS·OAuth와 같은 mock-first 패턴).
 */
public interface FileStorage {

    /**
     * 파일을 저장하고 접근 URL을 반환.
     * <p>파일을 저장하고 접근 URL을 반환한다.
     *
     * @param file 업로드된 파일(비어있지 않고 검증을 통과한 상태)
     * @return 저장된 파일의 접근 URL
     */
    String store(MultipartFile file);

    /**
     * 저장된 파일을 URL로 삭제한다.
     * <p><b>멱등하게 동작해야 한다</b> — 파일이 이미 없거나 우리가 저장한 URL이 아니면 조용히 무시한다.
     * 탈퇴처럼 여러 정리를 한 트랜잭션에서 수행하는 흐름이 파일 하나 때문에 실패하면 안 되기 때문이다.
     *
     * @param url {@link #store} 가 반환했던 접근 URL(null·빈 값 허용)
     */
    void delete(String url);
}
