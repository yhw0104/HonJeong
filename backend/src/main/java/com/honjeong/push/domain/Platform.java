package com.honjeong.push.domain;

/**
 * 기기 플랫폼. DB의 {@code device_tokens.platform} CHECK 제약과 값이 일치해야 한다.
 *
 * <p>ANDROID는 현재 앱이 발급하지 않는다(안드로이드 빌드 미지원). 나중에 붙일 때
 * 마이그레이션 없이 쓸 수 있도록 값만 미리 둔다.
 */
public enum Platform {
    IOS,
    ANDROID
}
