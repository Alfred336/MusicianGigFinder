package com.musicianfinder.models;

import com.google.firebase.firestore.DocumentId;
import java.util.List;

public class Musician {
    @DocumentId
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String gender;
    private String skillLevel;           // "Medium" or "Advanced"
    private boolean canPlayAll12Keys;    // Required for Advanced
    private String instrument;           // "Drums","Piano","Guitar","Others"
    private String workType;             // "Anywhere" or "Church only"
    private String region;
    private String country;
    private String profileImageUrl;
    private String bio;
    private String sampleMediaUrl;
    private String status;               // "Pending","Active","Suspended"
    private String availabilityStatus;   // "Available","Busy"
    private double rating;
    private int totalRatings;
    private long registeredAt;
    private List<String> gigHistory;
    private int totalGigsCompleted;
    private double totalEarnings;

    // ── Pricing constants ───────────────────────────────────────────────
    public static final int PRICE_MEDIUM   = 20_000;   // TZS
    public static final int PRICE_ADVANCED = 30_000;   // TZS

    public Musician() {}

    public Musician(String firstName, String lastName, String email, String phone,
                    String gender, String skillLevel, boolean canPlayAll12Keys,
                    String instrument, String workType, String region, String country) {
        this.firstName        = firstName;
        this.lastName         = lastName;
        this.email            = email;
        this.phone            = phone;
        this.gender           = gender;
        this.skillLevel       = skillLevel;
        this.canPlayAll12Keys = canPlayAll12Keys;
        this.instrument       = instrument;
        this.workType         = workType;
        this.region           = region;
        this.country          = country;
        this.status           = "Pending";
        this.availabilityStatus = "Available";
        this.rating           = 0.0;
        this.totalRatings     = 0;
        this.registeredAt     = System.currentTimeMillis();
    }

    // ── Computed helpers ────────────────────────────────────────────────
    public String getFullName()  { return firstName + " " + lastName; }
    public int    getGigPrice()  { return "Advanced".equals(skillLevel) ? PRICE_ADVANCED : PRICE_MEDIUM; }
    public String getLocation()  { return (country != null && !country.isEmpty()) ? country : region; }

    /** Advanced musicians MUST be able to play all 12 keys */
    public boolean meetsAdvancedCriteria() {
        return !"Advanced".equals(skillLevel) || canPlayAll12Keys;
    }

    // ── Getters & Setters ───────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }
    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String v) { this.skillLevel = v; }
    public boolean isCanPlayAll12Keys() { return canPlayAll12Keys; }
    public void setCanPlayAll12Keys(boolean v) { this.canPlayAll12Keys = v; }
    public String getInstrument() { return instrument; }
    public void setInstrument(String v) { this.instrument = v; }
    public String getWorkType() { return workType; }
    public void setWorkType(String v) { this.workType = v; }
    public String getRegion() { return region; }
    public void setRegion(String v) { this.region = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { this.country = v; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String v) { this.profileImageUrl = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { this.bio = v; }
    public String getSampleMediaUrl() { return sampleMediaUrl; }
    public void setSampleMediaUrl(String v) { this.sampleMediaUrl = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String v) { this.availabilityStatus = v; }
    public double getRating() { return rating; }
    public void setRating(double v) { this.rating = v; }
    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int v) { this.totalRatings = v; }
    public long getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(long v) { this.registeredAt = v; }
    public List<String> getGigHistory() { return gigHistory; }
    public void setGigHistory(List<String> v) { this.gigHistory = v; }
    public int getTotalGigsCompleted() { return totalGigsCompleted; }
    public void setTotalGigsCompleted(int v) { this.totalGigsCompleted = v; }
    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double v) { this.totalEarnings = v; }
}
