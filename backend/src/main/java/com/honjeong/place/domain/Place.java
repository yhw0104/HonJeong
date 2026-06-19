package com.honjeong.place.domain;

import com.honjeong.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 장소(식당) 엔티티. V1에서는 카카오 place id({@code external_id})를 캐싱 키로 사용했으나,
 * V2 이후 공공데이터 마스터로 전환 중이다. {@code external_id} 컬럼과 {@link #of} 팩토리는 Task 8(V3)에서
 * 호출부 정리 후 제거된다. 현재는 두 팩토리({@link #of}, {@link #ofPublicData})가 공존한다.
 *
 * <p>컬럼명은 기본 스네이크케이스 전략으로 매핑된다.
 */
@Entity
@Table(name = "places")
public class Place extends BaseTimeEntity {

    // PK. IDENTITY 전략 → DB의 auto-increment에 위임해 INSERT 시 채워진다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카카오 place id. V2에서 nullable로 완화(공공데이터 행은 없음). Task 8(V3)에서 컬럼 제거 예정.
    // @Column(nullable=false)를 제거해 V2 스키마(nullable)와 일치시킨다.
    private String externalId;

    // 데이터 출처 식별자. 'PUBLIC_DATA' 또는 향후 추가 출처. NOT NULL.
    @Column(nullable = false)
    private String source;

    // 출처별 관리번호. 공공데이터 적재 행의 식별 키 (source, source_id) UNIQUE.
    private String sourceId;

    // 가게명. NOT NULL.
    @Column(nullable = false)
    private String name;

    // 카테고리(예: 한식). nullable.
    private String category;

    // 지번 주소. nullable.
    private String address;

    // 도로명 주소. nullable.
    private String roadAddress;

    // 위도. NOT NULL.
    @Column(nullable = false)
    private double latitude;

    // 경도. NOT NULL.
    @Column(nullable = false)
    private double longitude;

    // 전화번호. nullable.
    private String phone;

    // 영업 상태 (예: '영업', '폐업'). nullable.
    private String businessStatus;

    /** JPA가 리플렉션으로 엔티티를 생성할 때 쓰는 기본 생성자. 외부 직접 호출은 막으려고 protected. */
    protected Place() {
    }

    /**
     * 레거시 카카오 캐시용 생성자. {@link #of} 팩토리에서만 호출한다.
     * source 는 'PUBLIC_DATA' 로 고정한다(V2 NOT NULL DEFAULT 충족).
     */
    private Place(String externalId, String name, String address, double latitude, double longitude, String category) {
        this.externalId = externalId;
        this.source = "PUBLIC_DATA";
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
    }

    /**
     * 공공데이터 마스터용 생성자. {@link #ofPublicData} 팩토리에서만 호출한다.
     */
    private Place(String source, String sourceId, String name, String category, String address,
            String roadAddress, double latitude, double longitude, String phone, String businessStatus) {
        this.source = source;
        this.sourceId = sourceId;
        this.name = name;
        this.category = category;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.businessStatus = businessStatus;
    }

    /**
     * 카카오 검색 결과로 새 캐시 엔티티를 만드는 레거시 팩토리. Task 8에서 제거 예정.
     *
     * @param externalId 카카오 place id(캐싱 키)
     * @param name       가게명
     * @param address    주소(nullable)
     * @param latitude   위도
     * @param longitude  경도
     * @param category   카테고리(nullable)
     * @return 새 Place 인스턴스
     */
    public static Place of(String externalId, String name, String address,
            double latitude, double longitude, String category) {
        return new Place(externalId, name, address, latitude, longitude, category);
    }

    /**
     * 공공데이터 원천으로 새 Place 엔티티를 만드는 팩토리.
     *
     * @param sourceId       출처별 관리번호
     * @param name           가게명
     * @param category       카테고리(nullable)
     * @param address        지번 주소(nullable)
     * @param roadAddress    도로명 주소(nullable)
     * @param latitude       위도
     * @param longitude      경도
     * @param phone          전화번호(nullable)
     * @param businessStatus 영업 상태(nullable)
     * @return 새 Place 인스턴스
     */
    public static Place ofPublicData(String sourceId, String name, String category, String address,
            String roadAddress, double latitude, double longitude, String phone, String businessStatus) {
        return new Place("PUBLIC_DATA", sourceId, name, category, address, roadAddress,
                latitude, longitude, phone, businessStatus);
    }

    /** 내부 식별자(PK)를 반환한다. */
    public Long getId() {
        return id;
    }

    /** 카카오 place id(캐싱 키)를 반환한다. Task 8에서 제거 예정. */
    public String getExternalId() {
        return externalId;
    }

    /** 데이터 출처 식별자를 반환한다. */
    public String getSource() {
        return source;
    }

    /** 출처별 관리번호를 반환한다(없으면 null). */
    public String getSourceId() {
        return sourceId;
    }

    /** 가게명을 반환한다. */
    public String getName() {
        return name;
    }

    /** 카테고리를 반환한다(없으면 null). */
    public String getCategory() {
        return category;
    }

    /** 지번 주소를 반환한다(없으면 null). */
    public String getAddress() {
        return address;
    }

    /** 도로명 주소를 반환한다(없으면 null). */
    public String getRoadAddress() {
        return roadAddress;
    }

    /** 위도를 반환한다. */
    public double getLatitude() {
        return latitude;
    }

    /** 경도를 반환한다. */
    public double getLongitude() {
        return longitude;
    }

    /** 전화번호를 반환한다(없으면 null). */
    public String getPhone() {
        return phone;
    }

    /** 영업 상태를 반환한다(없으면 null). */
    public String getBusinessStatus() {
        return businessStatus;
    }
}
