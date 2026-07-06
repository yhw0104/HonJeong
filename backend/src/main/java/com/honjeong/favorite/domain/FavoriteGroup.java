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

@Entity
@Table(name = "favorite_groups")
public class FavoriteGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(length = 120)
    private String note;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

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

    public static FavoriteGroup create(User user, String name, String note, String color, boolean isDefault) {
        return new FavoriteGroup(user, name, note, color, isDefault);
    }

    public static FavoriteGroup createDefault(User user) {
        return new FavoriteGroup(user, "기본 그룹", null, "#FF5A1F", true);
    }

    /** null 인자는 미변경(부분 수정). */
    public void updateInfo(String name, String note, String color) {
        if (name != null) this.name = name;
        if (note != null) this.note = note;
        if (color != null) this.color = color;
    }

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
