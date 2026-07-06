package com.honjeong.user.domain;

/**
 * 회원 성별. User.gender에 @Enumerated(EnumType.STRING)으로 이름 그대로 저장된다.
 */
public enum Gender {
    /** 남성 */
    MALE,
    /** 여성 */
    FEMALE,
    /** 선택 안 함/비공개 */
    NONE
}
