package com.honjeong.user.domain;

import java.time.LocalDate;

import com.honjeong.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 회원(사용자) — 휴대폰·닉네임·소개·활동 지역·식사성향 등 프로필과 가입 상태를 담는 데이터.
 * (엔티티, 매핑 테이블 users)
 *
 * <p>[기존 주석] 회원. 온보딩 시작 시 status=PENDING으로 생성되고(휴대폰/소셜 식별만), /auth/complete에서
 * 프로필을 채우며 status=ACTIVE로 전환된다. 컬럼명은 기본 스네이크케이스 전략으로 매핑된다.
 */
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    // PK. IDENTITY 전략 → DB의 auto-increment(시퀀스/자동 증가)에 위임해 INSERT 시 채워진다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 휴대폰 번호. 별도 @Column이 없으면 필드명을 스네이크케이스로 자동 매핑(phone).
    private String phone;
    // 이메일(소셜 로그인 시 공급자가 준 값이 들어올 수 있음). nullable.
    private String email;
    // 표시용 닉네임. 프로필 완료 단계에서 채워진다.
    private String nickname;
    // 프로필 이미지 URL. 카멜케이스 필드 → profile_image_url 컬럼으로 자동 매핑.
    private String profileImageUrl;

    // 성별. @Enumerated(EnumType.STRING) → enum 이름(MALE/FEMALE/NONE)을 문자열로 저장.
    // ordinal(순서 숫자) 대신 문자열로 두면 enum 값 순서를 바꿔도 기존 데이터가 깨지지 않는다.
    @Enumerated(EnumType.STRING)
    private Gender gender;

    // 생년월일(온보딩 고정). 표시 연령대는 birth_date로 파생, 응답엔 미노출.
    private LocalDate birthDate;
    // 자기소개 한 줄.
    private String introduction;
    // 활동 지역명(예: "서울 강남구").
    private String region;
    // 지역 좌표(위도). region_lat 컬럼으로 매핑.
    private Double regionLat;
    // 지역 좌표(경도). region_lng 컬럼으로 매핑.
    private Double regionLng;

    // 식사 성향(TALK/QUIET). 마찬가지로 문자열로 저장.
    @Enumerated(EnumType.STRING)
    private DiningStyle diningStyle;

    // 같이먹기 신청 수신 허용 여부(opt-in). 기본값 true, NOT NULL.
    @Column(nullable = false)
    private boolean allowMealRequest = true;

    // 회원 상태. 문자열 enum + NOT NULL. 생성 시 PENDING(온보딩 중), 프로필 완료 시 ACTIVE.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING;

    /** JPA가 리플렉션으로 엔티티를 생성할 때 쓰는 기본 생성자. 외부 직접 호출은 막으려고 protected. */
    protected User() {
    }

    /**
     * 내부 전용 생성자. 휴대폰/이메일만 받아 PENDING 상태로 초기화한다.
     * 외부에서는 {@link #pending(String, String)} 팩토리로만 생성하도록 private으로 막는다.
     *
     * @param phone 휴대폰 번호(없으면 null 가능)
     * @param email 이메일(없으면 null 가능)
     */
    private User(String phone, String email) {
        this.phone = phone;
        this.email = email;
        this.allowMealRequest = true;
        this.status = UserStatus.PENDING;
    }

    /**
     * 온보딩 시작용 PENDING 회원을 생성하는 정적 팩토리.
     * 휴대폰/이메일은 알면 채우고 없으면 null로 둔다. 생성 시점의 status는 항상 PENDING.
     *
     * @param phone 휴대폰 번호(nullable)
     * @param email 이메일(nullable)
     * @return PENDING 상태로 초기화된 새 User 인스턴스
     */
    public static User pending(String phone, String email) {
        return new User(phone, email);
    }

    /**
     * 온보딩 중 휴대폰 인증을 끝냈을 때 번호를 채운다.
     * 소셜 로그인으로 먼저 들어와 회원이 만들어진(OAuth 선진입) 뒤 휴대폰을 나중에 인증하는 경로에서 쓴다.
     *
     * @param phone 인증 완료된 휴대폰 번호
     */
    public void assignPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 프로필 셋업을 완료하며 가입을 확정한다. 전달된 프로필 값들을 모두 채운 뒤
     * status를 PENDING → ACTIVE로 전환한다(이 호출 이후 {@link #isActive()}가 true).
     *
     * @param nickname        닉네임
     * @param gender          성별
     * @param birthDate       생년월일
     * @param introduction    자기소개
     * @param region          지역명
     * @param regionLat       지역 위도
     * @param regionLng       지역 경도
     * @param diningStyle     식사 성향
     * @param profileImageUrl 프로필 이미지 URL
     */
    public void completeProfile(String nickname, Gender gender, LocalDate birthDate, String introduction,
            String region, Double regionLat, Double regionLng, DiningStyle diningStyle, String profileImageUrl) {
        this.nickname = nickname;
        this.gender = gender;
        this.birthDate = birthDate;
        this.introduction = introduction;
        this.region = region;
        this.regionLat = regionLat;
        this.regionLng = regionLng;
        this.diningStyle = diningStyle;
        this.profileImageUrl = profileImageUrl;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 탈퇴 처리 — 개인정보 필드를 전부 비우고 상태를 WITHDRAWN으로 바꾼다(익명화).
     *
     * <p>행 자체는 남긴다. 리뷰·체크인·대화가 이 행을 참조하고 있어 삭제하면 식당 리뷰와 통계,
     * 상대방의 대화가 함께 사라지기 때문이다. 닉네임이 null이 되므로 표시 계층이 '알 수 없음' 같은
     * 대체 문구를 보여줘야 한다(현재는 이 처리가 없어 화면에 null이 그대로 노출될 수 있다 — 별도 작업 필요).
     *
     * <p>phone·nickname이 null이 되면서 재가입 경로가 자연히 열린다 — Postgres는 NULL을 UNIQUE
     * 중복으로 치지 않으므로 탈퇴자가 여럿이어도 공존하고, 같은 번호로 다시 가입하면 새 행이 만들어진다.
     */
    public void withdraw() {
        this.phone = null;
        this.email = null;
        this.nickname = null;
        this.profileImageUrl = null;
        this.gender = null;
        this.birthDate = null;
        this.introduction = null;
        this.region = null;
        this.regionLat = null;
        this.regionLng = null;
        this.diningStyle = null;
        this.allowMealRequest = false;
        this.status = UserStatus.WITHDRAWN;
    }

    /**
     * 프로필을 부분 수정한다(PATCH). 전달된 값 중 <b>null이 아닌 필드만</b> 반영하고, null인 필드는 기존 값을
     * 보존한다. 빈 문자열("")은 "해당 필드를 비움"으로 취급해 그대로 반영한다. 닉네임 중복 검사는 호출 측
     * (서비스)에서 미리 수행한다. {@code gender}·{@code birthDate}는 수정 대상이 아니다(온보딩 시 고정).
     *
     * @param nickname        새 닉네임(선택)
     * @param profileImageUrl 새 프로필 이미지 URL(선택)
     * @param introduction    새 한 줄 소개(선택, ""로 비우기 가능)
     * @param region          새 활동 지역명(선택)
     * @param regionLat       새 지역 위도(선택)
     * @param regionLng       새 지역 경도(선택)
     * @param diningStyle     새 식사 성향(선택)
     * @param allowMealRequest 같이먹기 수신 허용 토글(선택). 엔티티 필드는 primitive지만 부분수정을 위해 Boolean으로 받는다.
     */
    public void updateProfile(String nickname, String profileImageUrl, String introduction,
            String region, Double regionLat, Double regionLng, DiningStyle diningStyle, Boolean allowMealRequest) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (introduction != null) this.introduction = introduction;
        if (region != null) this.region = region;
        if (regionLat != null) this.regionLat = regionLat;
        if (regionLng != null) this.regionLng = regionLng;
        if (diningStyle != null) this.diningStyle = diningStyle;
        if (allowMealRequest != null) this.allowMealRequest = allowMealRequest;
    }

    /**
     * 가입이 확정된 ACTIVE 회원인지 판정한다.
     *
     * @return status가 ACTIVE면 true(아직 온보딩 중이거나 정지/탈퇴면 false)
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    // --- 이하 게터: 필드 값을 읽기 전용으로 노출(상태 변경 없음) ---

    public Long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getIntroduction() {
        return introduction;
    }

    public String getRegion() {
        return region;
    }

    public Double getRegionLat() {
        return regionLat;
    }

    public Double getRegionLng() {
        return regionLng;
    }

    public DiningStyle getDiningStyle() {
        return diningStyle;
    }

    public boolean isAllowMealRequest() {
        return allowMealRequest;
    }

    public UserStatus getStatus() {
        return status;
    }
}
