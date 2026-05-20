package com.cloudstorage.model;

/**
 * Модель пользователя системы
 */
public class User {
    private Integer id;
    private String login;
    private String email;
    private String passwordHash;
    private String role;
    private long quota;
    private long usedSpace;

    public User() {}

    public User(Integer id, String login, String email, String role, long quota, long usedSpace) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.role = role;
        this.quota = quota;
        this.usedSpace = usedSpace;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getQuota() { return quota; }
    public void setQuota(long quota) { this.quota = quota; }

    public long getUsedSpace() { return usedSpace; }
    public void setUsedSpace(long usedSpace) { this.usedSpace = usedSpace; }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
