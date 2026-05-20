package com.cloudstorage.model;

import java.time.Instant;

/**
 * Модель папки
 */
public class Folder {
    private Integer id;
    private Integer userId;
    private String name;
    private Integer parentId;
    private boolean deleted;
    private Instant createdAt;

    public Folder() {}

    public Folder(Integer id, Integer userId, String name, Integer parentId) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.parentId = parentId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
