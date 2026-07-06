package com.honjeong.favorite.domain;

import java.util.ArrayList;
import java.util.List;

import com.honjeong.global.common.BaseTimeEntity;
import com.honjeong.user.domain.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * 사용자의 즐겨찾기 그룹(장소 묶음 폴더 — 가입 시 "기본 그룹" 1개 자동 생성)을 나타내는 엔티티.
 * (엔티티: 매핑 테이블 favorite_groups, 생성/수정 시각은 BaseTimeEntity 상속)
 */
@Entity
@Table(name = "favorite_groups")
public class FavoriteGroup extends BaseTimeEntity {

    /** 그룹 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 그룹 소유 사용자 (FK: user_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 그룹 이름 (최대 40자, 기본 그룹은 "기본 그룹") */
    @Column(nullable = false, length = 40)
    private String name;

    /** 그룹 메모 (선택, 최대 120자) */
    @Column(length = 120)
    private String note;

    /** 그룹 표시 색상 (HEX 문자열, 미지정 시 기본 #FF5A1F) */
    @Column(nullable = false, length = 20)
    private String color;

    /** 기본 그룹 여부 (가입 시 자동 생성된 그룹 — 삭제 불가) */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    /** 그룹에 담긴 즐겨찾기 목록 (그룹 삭제 시 함께 삭제 — cascade + orphanRemoval) */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    protected FavoriteGroup() {}

    private FavoriteGroup(User user, String name, String note, String color, boolean isDefault) {
        this.user = user;
        this.name = name;
        this.note = note;
        this.color = color;
        this.isDefault = isDefault;
    }

    /**
     * 기능: 즐겨찾기 그룹 인스턴스 생성(정적 팩토리)
     * Request: user — 소유 사용자, name — 그룹 이름, note — 메모, color — 색상, isDefault — 기본 그룹 여부
     * Response: FavoriteGroup — 저장 전 새 그룹 엔티티
     */
    public static FavoriteGroup create(User user, String name, String note, String color, boolean isDefault) {
        return new FavoriteGroup(user, name, note, color, isDefault);
    }

    /**
     * 기능: 가입 시 자동 생성되는 기본 그룹("기본 그룹", 기본 색 #FF5A1F, isDefault=true) 생성(정적 팩토리)
     * Request: user — 소유 사용자
     * Response: FavoriteGroup — 저장 전 기본 그룹 엔티티
     */
    public static FavoriteGroup createDefault(User user) {
        return new FavoriteGroup(user, "기본 그룹", null, "#FF5A1F", true);
    }

    /**
     * 기능: 그룹의 이름/메모/색상을 부분 수정
     * Request: name — 새 이름, note — 새 메모, color — 새 색상 (각각 null이면 미변경)
     * Response: 없음(void)
     *
     * <p>[기존 주석] null 인자는 미변경(부분 수정).
     */
    public void updateInfo(String name, String note, String color) {
        if (name != null) this.name = name;
        if (note != null) this.note = note;
        if (color != null) this.color = color;
    }

    /**
     * 기능: 그룹이 해당 사용자 소유인지 확인(소유권 검증용)
     * Request: userId — 확인할 사용자 ID
     * Response: boolean — 소유 여부
     */
    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public String getNote() { return note; }
    public String getColor() { return color; }
    public boolean isDefault() { return isDefault; }
}
