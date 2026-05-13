package com.musicianfinder.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;
import com.musicianfinder.models.GigRequest;
import com.musicianfinder.models.Musician;
import com.musicianfinder.models.Payment;
import com.musicianfinder.models.Review;

public class FirebaseHelper {

    // Firestore collection names
    public static final String COL_MUSICIANS  = "musicians";
    public static final String COL_REQUESTS   = "gig_requests";
    public static final String COL_PAYMENTS   = "payments";
    public static final String COL_REVIEWS    = "reviews";
    public static final String COL_ADMINS     = "admins";

    // SharedPreferences key
    public static final String PREFS_NAME     = "MusicianGigPrefs";
    public static final String KEY_ROLE       = "user_role";       // "musician" | "admin"
    public static final String KEY_DARK_MODE  = "dark_mode";

    // Pricing
    public static final int PRICE_MEDIUM   = 10_000;
    public static final int PRICE_ADVANCED = 20_000;

    // Singleton instances
    private static FirebaseHelper instance;
    private final FirebaseAuth       auth;
    private final FirebaseFirestore  db;

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) instance = new FirebaseHelper();
        return instance;
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    public FirebaseAuth getAuth()          { return auth; }
    public FirebaseFirestore getDb()       { return db; }
    public FirebaseUser getCurrentUser()   { return auth.getCurrentUser(); }
    public boolean isLoggedIn()            { return auth.getCurrentUser() != null; }

    /** Register musician with Firebase Auth, then write Firestore doc. */
    public void registerMusician(Musician musician, String password,
                                 OnCompleteListener<String> listener) {
        auth.createUserWithEmailAndPassword(musician.getEmail(), password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    musician.setId(uid);
                    db.collection(COL_MUSICIANS).document(uid)
                            .set(musician)
                            .addOnSuccessListener(v -> {
                                subscribeToTopic("musician_" + uid);
                                listener.onSuccess(uid);
                            })
                            .addOnFailureListener(listener::onFailure);
                })
                .addOnFailureListener(listener::onFailure);
    }

    /** Login existing musician. */
    public void loginMusician(String email, String password, OnCompleteListener<String> listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> listener.onSuccess(r.getUser().getUid()))
                .addOnFailureListener(listener::onFailure);
    }

    /** Admin login (email/password stored in Firestore admins collection). */
    public void loginAdmin(String email, String password, OnCompleteListener<String> listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();
                    db.collection(COL_ADMINS).document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) listener.onSuccess(uid);
                                else {
                                    auth.signOut();
                                    listener.onFailure(new Exception("Not an admin account"));
                                }
                            })
                            .addOnFailureListener(listener::onFailure);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void logout() { auth.signOut(); }

    // ── Musician queries ──────────────────────────────────────────────────────

    public Query getTanzaniaMusicians(String region) {
        Query q = db.collection(COL_MUSICIANS).whereEqualTo("status", "Active");
        if (region != null && !region.isEmpty())
            q = q.whereEqualTo("region", region);
        return q;
    }

    public Query getInternationalMusicians(String country) {
        Query q = db.collection(COL_MUSICIANS)
                .whereEqualTo("status", "Active")
                .whereEqualTo("country", country != null ? country : "");
        return q;
    }

    public Query searchMusicians(String instrument, String skillLevel) {
        Query q = db.collection(COL_MUSICIANS).whereEqualTo("status", "Active");
        if (instrument != null && !instrument.equals("All"))
            q = q.whereEqualTo("instrument", instrument);
        if (skillLevel != null && !skillLevel.equals("All"))
            q = q.whereEqualTo("skillLevel", skillLevel);
        return q;
    }

    public Query getPendingMusicians() {
        return db.collection(COL_MUSICIANS).whereEqualTo("status", "Pending")
                .orderBy("registeredAt", Query.Direction.DESCENDING);
    }

    // ── Gig request operations ────────────────────────────────────────────────

    public void submitGigRequest(GigRequest request, OnCompleteListener<String> listener) {
        DocumentReference ref = db.collection(COL_REQUESTS).document();
        request.setId(ref.getId());
        ref.set(request)
                .addOnSuccessListener(v -> listener.onSuccess(ref.getId()))
                .addOnFailureListener(listener::onFailure);
    }

    public Query getPendingRequests() {
        return db.collection(COL_REQUESTS).whereEqualTo("status", "Pending")
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    public Query getMusicianRequests(String musicianId) {
        return db.collection(COL_REQUESTS).whereEqualTo("musicianId", musicianId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    public void updateRequestStatus(String requestId, String status, String adminNotes,
                                    OnCompleteListener<Void> listener) {
        db.collection(COL_REQUESTS).document(requestId)
                .update("status", status, "adminNotes", adminNotes)
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }

    // ── Payment operations ────────────────────────────────────────────────────

    public void submitPayment(Payment payment, OnCompleteListener<String> listener) {
        DocumentReference ref = db.collection(COL_PAYMENTS).document();
        payment.setId(ref.getId());
        ref.set(payment)
                .addOnSuccessListener(v -> listener.onSuccess(ref.getId()))
                .addOnFailureListener(listener::onFailure);
    }

    public void confirmPayment(String paymentId, String adminUid, OnCompleteListener<Void> listener) {
        db.collection(COL_PAYMENTS).document(paymentId)
                .update("status", "Confirmed",
                        "confirmedAt", System.currentTimeMillis(),
                        "confirmedBy", adminUid)
                .addOnSuccessListener(v -> listener.onSuccess(null))
                .addOnFailureListener(listener::onFailure);
    }

    public Query getPendingPayments() {
        return db.collection(COL_PAYMENTS).whereEqualTo("status", "Pending")
                .orderBy("paidAt", Query.Direction.DESCENDING);
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    public void submitReview(Review review, Musician musician, OnCompleteListener<Void> listener) {
        DocumentReference ref = db.collection(COL_REVIEWS).document();
        review.setId(ref.getId());

        // Recalculate musician rating
        double newRating = ((musician.getRating() * musician.getTotalRatings()) + review.getRating())
                / (musician.getTotalRatings() + 1);

        db.runTransaction(tx -> {
            tx.set(ref, review);
            tx.update(db.collection(COL_MUSICIANS).document(musician.getId()),
                    "rating", newRating,
                    "totalRatings", musician.getTotalRatings() + 1);
            return null;
        }).addOnSuccessListener(listener::onSuccess)
          .addOnFailureListener(listener::onFailure);
    }

    public Query getMusicianReviews(String musicianId) {
        return db.collection(COL_REVIEWS).whereEqualTo("musicianId", musicianId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    // ── Musician account management ───────────────────────────────────────────

    public void approveMusician(String musicianId, OnCompleteListener<Void> listener) {
        db.collection(COL_MUSICIANS).document(musicianId)
                .update("status", "Active")
                .addOnSuccessListener(v -> {
                    sendNotificationToTopic("musician_" + musicianId,
                            "Account Approved 🎉",
                            "Your musician account has been approved! You can now receive gigs.");
                    listener.onSuccess(null);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void suspendMusician(String musicianId, OnCompleteListener<Void> listener) {
        db.collection(COL_MUSICIANS).document(musicianId)
                .update("status", "Suspended")
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }

    public void updateAvailability(String musicianId, String status,
                                   OnCompleteListener<Void> listener) {
        db.collection(COL_MUSICIANS).document(musicianId)
                .update("availabilityStatus", status)
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }

    // ── FCM helpers ───────────────────────────────────────────────────────────

    public void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic);
    }

    /** NOTE: Real push notifications require a server-side call to FCM HTTP API.
     *  This is a placeholder; implement via Cloud Functions or your backend. */
    public void sendNotificationToTopic(String topic, String title, String body) {
        // TODO: Call Cloud Functions / backend endpoint
        // POST https://fcm.googleapis.com/fcm/send  { to: "/topics/<topic>", ... }
    }

    // ── SharedPreferences ─────────────────────────────────────────────────────

    public void saveRole(Context ctx, String role) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_ROLE, role).apply();
    }

    public String getRole(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ROLE, "");
    }

    public boolean isDarkMode(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    // ── Callback interface ────────────────────────────────────────────────────

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
// V2 patch — COL_PAYMENTS and confirmPayment already defined above
