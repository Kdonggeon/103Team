package com.rubypaper.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Hyperlink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long groupId;
    private String title;
    private String url;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getter / Setter
    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
