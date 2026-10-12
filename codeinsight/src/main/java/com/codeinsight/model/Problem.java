package com.codeinsight.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;          // owner of this problem
    private String title;
    private String platform;      // LeetCode, Codeforces, CodeChef, ...
    private String difficulty;    // Easy, Medium, Hard
    private String topic;         // Arrays, DP, Graphs, ...
    private String status;        // Solved, Attempted, Todo
    private String link;
    private LocalDate solvedDate;      // set automatically when status becomes Solved
    private LocalDate nextReviewDate;   // spaced repetition
    private Integer reviewCount;
    private Boolean needsRevision;     // flag to revisit later

    @Column(length = 1000)
    private String notes;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public LocalDate getSolvedDate() { return solvedDate; }
    public void setSolvedDate(LocalDate solvedDate) { this.solvedDate = solvedDate; }
    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate d) { this.nextReviewDate = d; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer c) { this.reviewCount = c; }
    public Boolean getNeedsRevision() { return needsRevision; }
    public void setNeedsRevision(Boolean needsRevision) { this.needsRevision = needsRevision; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
