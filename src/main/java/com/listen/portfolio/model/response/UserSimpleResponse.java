package com.listen.portfolio.model.response;

public class UserSimpleResponse {
    private Long id;
    private String name;
    private String location;
    private String email;
    private String avatarUrl;

    // Default constructor
    public UserSimpleResponse() {
    }

    // Constructor to map from the UserInfoResponse entity
    public UserSimpleResponse(UserInfoResponse user) {
        this.id = user.getId();
        this.name = user.getName();
        this.location = user.getLocation();
        this.email = user.getEmail();
        this.avatarUrl = user.getAvatarUrl();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
