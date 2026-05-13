package com.musicianfinder.models;

public class GigRequest {
    private String id;
    private String musicianId;
    private String musicianName;
    private String requesterName;
    private String requesterPhone;
    private String eventName;
    private String eventDate;
    private String eventTime;
    private String eventLocation;
    private String notes;
    private String status;          // "Pending", "Approved", "Rejected", "Completed"
    private String paymentStatus;   // "Pending", "Confirmed", "Rejected"
    private String paymentReference;
    private int amount;
    private long createdAt;
    private String adminNotes;

    public GigRequest() {}

    public GigRequest(String musicianId, String musicianName, String requesterName,
                      String requesterPhone, String eventName, String eventDate,
                      String eventTime, String eventLocation, String notes, int amount) {
        this.musicianId = musicianId;
        this.musicianName = musicianName;
        this.requesterName = requesterName;
        this.requesterPhone = requesterPhone;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.eventLocation = eventLocation;
        this.notes = notes;
        this.amount = amount;
        this.status = "Pending";
        this.paymentStatus = "Pending";
        this.createdAt = System.currentTimeMillis();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMusicianId() { return musicianId; }
    public void setMusicianId(String musicianId) { this.musicianId = musicianId; }
    public String getMusicianName() { return musicianName; }
    public void setMusicianName(String musicianName) { this.musicianName = musicianName; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public String getRequesterPhone() { return requesterPhone; }
    public void setRequesterPhone(String requesterPhone) { this.requesterPhone = requesterPhone; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }
    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }
    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
