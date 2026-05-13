package com.musicianfinder.models;

public class Review {
    private String id;
    private String musicianId;
    private String requesterName;
    private float rating;
    private String comment;
    private long createdAt;

    public Review() {}

    public Review(String musicianId, String requesterName, float rating, String comment) {
        this.musicianId = musicianId;
        this.requesterName = requesterName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMusicianId() { return musicianId; }
    public void setMusicianId(String musicianId) { this.musicianId = musicianId; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
