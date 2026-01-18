package com.listen.gallery.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_info")
public class UserInfo {

    @Id
    private Integer userId;

    private String userName;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}