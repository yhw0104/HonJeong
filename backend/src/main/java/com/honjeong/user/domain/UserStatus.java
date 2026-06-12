package com.honjeong.user.domain;

/**
 * 회원 상태. PENDING = 온보딩 진행 중(휴대폰/닉네임 등 미완), ACTIVE = 가입 완료.
 * SUSPENDED/WITHDRAWN = 정지/탈퇴.
 */
public enum UserStatus {
    PENDING,   // 온보딩 진행 중(휴대폰/닉네임 등 프로필 미완)
    ACTIVE,    // 프로필 완료·가입 확정(정상 이용 가능)
    SUSPENDED, // 정지(관리상 제재)
    WITHDRAWN  // 탈퇴
}
