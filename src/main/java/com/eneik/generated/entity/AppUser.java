package com.eneik.generated.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TGAccount> tgAccounts = new ArrayList<>();

    public AppUser() {}

    public AppUser(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<TGAccount> getTgAccounts() {
        return tgAccounts;
    }

    public void setTgAccounts(List<TGAccount> tgAccounts) {
        this.tgAccounts = tgAccounts;
    }
}
