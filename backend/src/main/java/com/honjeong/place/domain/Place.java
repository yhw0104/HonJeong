package com.honjeong.place.domain;

import com.honjeong.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 장소(식당) 캐시. 카카오 로컬 검색 결과를 우리 DB가 영구 소유하는 형태로, 체크인·리뷰 등 장소 UGC가 참조하는
 * 기준 엔티티다. 외부 카카오 place id({@code external_id})를 캐싱 키로 삼아, 체크인 시 없으면 INSERT,
 * 있으면 재사용(upsert)한다. 컬럼명은 기본 스네이크케이스 전략으로 매핑된다.
 *
 * <p>{@code phone}·{@code homepage_url} 컬럼은 P1에서 쓰지 않아 매핑하지 않는다(스키마 검증은 매핑된 컬럼만 보므로
 * 무방하며, 출처가 확정되는 P2 상세 기능에서 필요해질 때 매핑을 추가한다).
 */
@Entity
@Table(name = "places")
public class Place extends BaseTimeEntity {

    // PK. IDENTITY 전략 → DB의 auto-increment에 위임해 INSERT 시 채워진다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카카오 place id. 캐싱 upsert 키이며 DB에서 UNIQUE·NOT NULL이다.
    @Column(nullable = false)
    private String externalId;

    // 가게명. NOT NULL.
    @Column(nullable = false)
    private String name;

    // 주소. 카카오가 안 줄 수 있어 nullable.
    private String address;

    // 위도. 지도·반경검색에 쓰여 NOT NULL. 결측이 없으므로 원시 double로 둔다.
    @Column(nullable = false)
    private double latitude;

    // 경도. 위와 동일하게 NOT NULL·원시 double.
    @Column(nullable = false)
    private double longitude;

    // 카테고리(예: 한식). nullable.
    private String category;

    /** JPA가 리플렉션으로 엔티티를 생성할 때 쓰는 기본 생성자. 외부 직접 호출은 막으려고 protected. */
    protected Place() {
    }

    /**
     * 내부 전용 생성자. 외부에서는 {@link #of} 팩토리로만 생성하도록 private으로 막는다.
     *
     * @param externalId 카카오 place id(캐싱 키)
     * @param name       가게명
     * @param address    주소(nullable)
     * @param latitude   위도
     * @param longitude  경도
     * @param category   카테고리(nullable)
     */
    private Place(String externalId, String name, String address, double latitude, double longitude, String category) {
        this.externalId = externalId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
    }

    /**
     * 검색 결과(또는 체크인 요청)로 전달된 장소 정보로 새 캐시 엔티티를 만드는 정적 팩토리.
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

    /** 내부 식별자(PK)를 반환한다. */
    public Long getId() {
        return id;
    }

    /** 카카오 place id(캐싱 키)를 반환한다. */
    public String getExternalId() {
        return externalId;
    }

    /** 가게명을 반환한다. */
    public String getName() {
        return name;
    }

    /** 주소를 반환한다(없으면 null). */
    public String getAddress() {
        return address;
    }

    /** 위도를 반환한다. */
    public double getLatitude() {
        return latitude;
    }

    /** 경도를 반환한다. */
    public double getLongitude() {
        return longitude;
    }

    /** 카테고리를 반환한다(없으면 null). */
    public String getCategory() {
        return category;
    }
}
