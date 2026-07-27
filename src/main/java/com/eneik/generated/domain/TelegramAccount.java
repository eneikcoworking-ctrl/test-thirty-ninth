package com.eneik.generated.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "telegram_accounts")
public class TelegramAccount {

    @Id
    private String id;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String username;

    private String status;

    public TelegramAccount() {}

    public TelegramAccount(String id, String phoneNumber, String username, String status) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
