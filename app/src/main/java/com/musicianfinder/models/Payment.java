package com.musicianfinder.models;

public class Payment {
    // ── Payment method types ────────────────────────────────────────────
    public static final String METHOD_MPESA      = "M-Pesa (Vodacom)";
    public static final String METHOD_AIRTEL     = "Airtel Money";
    public static final String METHOD_MIXX       = "Mixx by Yas";
    public static final String METHOD_HALOPESA   = "HaloPesa";
    public static final String METHOD_EZYPESA    = "EzyPesa";
    public static final String METHOD_MASTERCARD = "Mastercard";
    public static final String METHOD_VISA       = "Visa";
    public static final String METHOD_UNIONPAY   = "UnionPay";

    // ── ClickPesa provider codes ────────────────────────────────────────
    public static final String CP_VODACOM  = "VODACOM";
    public static final String CP_AIRTEL   = "AIRTEL";
    public static final String CP_TIGO     = "TIGO";
    public static final String CP_HALOTEL  = "HALOTEL";
    public static final String CP_TNMPESA  = "TNMPESA";   // EzyPesa / TTCL
    public static final String CP_CARD     = "CARD";

    // ── Statuses ────────────────────────────────────────────────────────
    public static final String STATUS_PENDING   = "Pending";
    public static final String STATUS_CONFIRMED = "Confirmed";
    public static final String STATUS_REJECTED  = "Rejected";
    public static final String STATUS_FAILED    = "Failed";

    private String id;
    private String gigRequestId;
    private String musicianId;
    private String musicianName;
    private String requesterName;
    private String requesterPhone;
    private int    amount;
    private String paymentMethod;
    private String clickpesaProvider;     // ClickPesa internal provider code
    private String clickpesaTransactionId;
    private String referenceNumber;
    private String status;
    private long   paidAt;
    private long   confirmedAt;
    private String confirmedBy;           // Admin UID
    // Card-specific (masked after submission)
    private String cardLast4;
    private String cardBrand;

    public Payment() {}

    /** Mobile money constructor */
    public Payment(String gigRequestId, String musicianId, String musicianName,
                   String requesterName, String requesterPhone, int amount,
                   String paymentMethod, String clickpesaProvider) {
        this.gigRequestId      = gigRequestId;
        this.musicianId        = musicianId;
        this.musicianName      = musicianName;
        this.requesterName     = requesterName;
        this.requesterPhone    = requesterPhone;
        this.amount            = amount;
        this.paymentMethod     = paymentMethod;
        this.clickpesaProvider = clickpesaProvider;
        this.status            = STATUS_PENDING;
        this.paidAt            = System.currentTimeMillis();
    }

    /** Card payment constructor */
    public Payment(String gigRequestId, String musicianId, String musicianName,
                   String requesterName, int amount, String cardBrand, String cardLast4) {
        this.gigRequestId   = gigRequestId;
        this.musicianId     = musicianId;
        this.musicianName   = musicianName;
        this.requesterName  = requesterName;
        this.amount         = amount;
        this.paymentMethod  = cardBrand;
        this.clickpesaProvider = CP_CARD;
        this.cardBrand      = cardBrand;
        this.cardLast4      = cardLast4;
        this.status         = STATUS_PENDING;
        this.paidAt         = System.currentTimeMillis();
    }

    public boolean isMobileMoney() {
        return !CP_CARD.equals(clickpesaProvider);
    }

    // ── Getters & Setters ───────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getGigRequestId() { return gigRequestId; }
    public void setGigRequestId(String v) { this.gigRequestId = v; }
    public String getMusicianId() { return musicianId; }
    public void setMusicianId(String v) { this.musicianId = v; }
    public String getMusicianName() { return musicianName; }
    public void setMusicianName(String v) { this.musicianName = v; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String v) { this.requesterName = v; }
    public String getRequesterPhone() { return requesterPhone; }
    public void setRequesterPhone(String v) { this.requesterPhone = v; }
    public int getAmount() { return amount; }
    public void setAmount(int v) { this.amount = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public String getClickpesaProvider() { return clickpesaProvider; }
    public void setClickpesaProvider(String v) { this.clickpesaProvider = v; }
    public String getClickpesaTransactionId() { return clickpesaTransactionId; }
    public void setClickpesaTransactionId(String v) { this.clickpesaTransactionId = v; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String v) { this.referenceNumber = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public long getPaidAt() { return paidAt; }
    public void setPaidAt(long v) { this.paidAt = v; }
    public long getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(long v) { this.confirmedAt = v; }
    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String v) { this.confirmedBy = v; }
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String v) { this.cardLast4 = v; }
    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String v) { this.cardBrand = v; }
}
