package com.honjeong.place.domain;

import com.honjeong.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 장소(식당) 엔티티. V3 이후 공공데이터 마스터만 사용한다({@code external_id} 컬럼은 V3 마이그레이션으로 제거됨).
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
