package com.musicianfinder.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.musicianfinder.R;
import com.musicianfinder.models.GigRequest;
import com.musicianfinder.models.Musician;
import com.musicianfinder.models.Payment;
import com.musicianfinder.utils.FirebaseHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    // Stats
    private TextView tvTotalMusicians, tvActiveMusicians, tvPendingMusicians;
    private TextView tvTotalRequests,  tvPendingRequests;
    private TextView tvTotalPayments,  tvPendingPayments, tvTotalRevenue;

    // Lists
    private RecyclerView rvPendingMusicians, rvPendingRequests, rvPendingPayments;
    private ProgressBar  progressBar;

    private FirebaseHelper      fb;
    private List<Musician>      pendingMusicianList = new ArrayList<>();
    private List<GigRequest>    pendingRequestList  = new ArrayList<>();
    private List<Payment>       pendingPaymentList  = new ArrayList<>();

    private static final NumberFormat TZS = NumberFormat.getNumberInstance(Locale.US);
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Admin Dashboard");

        fb = FirebaseHelper.getInstance();
        bindViews();
        loadDashboard();
    }

    private void bindViews() {
        tvTotalMusicians   = findViewById(R.id.tvTotalMusicians);
        tvActiveMusicians  = findViewById(R.id.tvActiveMusicians);
        tvPendingMusicians = findViewById(R.id.tvPendingMusicians);
        tvTotalRequests    = findViewById(R.id.tvTotalRequests);
        tvPendingRequests  = findViewById(R.id.tvPendingRequests);
        tvTotalPayments    = findViewById(R.id.tvTotalPayments);
        tvPendingPayments  = findViewById(R.id.tvPendingPayments);
        tvTotalRevenue     = findViewById(R.id.tvTotalRevenue);
        progressBar        = findViewById(R.id.progressBar);
        rvPendingMusicians = findViewById(R.id.rvPendingMusicians);
        rvPendingRequests  = findViewById(R.id.rvPendingRequests);
        rvPendingPayments  = findViewById(R.id.rvPendingPayments);

        rvPendingMusicians.setLayoutManager(new LinearLayoutManager(this));
        rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        rvPendingPayments.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadDashboard() {
        progressBar.setVisibility(View.VISIBLE);
        loadStats();
        loadPendingMusicians();
        loadPendingRequests();
        loadPendingPayments();
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void loadStats() {
        fb.getDb().collection(FirebaseHelper.COL_MUSICIANS).get()
                .addOnSuccessListener(snap -> {
                    int total = snap.size(), active = 0, pending = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String s = d.getString("status");
                        if ("Active".equals(s))  active++;
                        if ("Pending".equals(s)) pending++;
                    }
                    tvTotalMusicians.setText("Total Musicians: " + total);
                    tvActiveMusicians.setText("Active: " + active);
                    tvPendingMusicians.setText("Pending Approval: " + pending);
                });

        fb.getDb().collection(FirebaseHelper.COL_REQUESTS).get()
                .addOnSuccessListener(snap -> {
                    int total = snap.size(), pending = 0;
                    for (DocumentSnapshot d : snap.getDocuments())
                        if ("Pending".equals(d.getString("status"))) pending++;
                    tvTotalRequests.setText("Total Gig Requests: " + total);
                    tvPendingRequests.setText("Pending: " + pending);
                });

        fb.getDb().collection(FirebaseHelper.COL_PAYMENTS).get()
                .addOnSuccessListener(snap -> {
                    int total = snap.size(), pending = 0;
                    long revenue = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if ("Pending".equals(d.getString("status")))   pending++;
                        if ("Confirmed".equals(d.getString("status"))) {
                            Long amt = d.getLong("amount");
                            if (amt != null) revenue += amt;
                        }
                    }
                    tvTotalPayments.setText("Total Payments: " + total);
                    tvPendingPayments.setText("Pending Confirmation: " + pending);
                    tvTotalRevenue.setText("Confirmed Revenue: TZS " + TZS.format(revenue));
                    progressBar.setVisibility(View.GONE);
                });
    }

    // ── Pending musicians ─────────────────────────────────────────────────────

    private void loadPendingMusicians() {
        fb.getPendingMusicians().addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            pendingMusicianList.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Musician m = doc.toObject(Musician.class);
                if (m != null) pendingMusicianList.add(m);
            }
            setupMusicianAdapter();
        });
    }

    private void setupMusicianAdapter() {
        rvPendingMusicians.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public int getItemCount() { return pendingMusicianList.size(); }
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_admin_musician, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                Musician m = pendingMusicianList.get(pos);
                String badge = m.isCanPlayAll12Keys() ? "  🎹 All 12 keys ✓" : "";
                ((TextView) h.itemView.findViewById(R.id.tvName)).setText(
                        m.getFullName() + " — " + m.getInstrument() +
                        " (" + m.getSkillLevel() + ")" + badge);
                ((TextView) h.itemView.findViewById(R.id.tvPhone)).setText(
                        "📞 " + m.getPhone() + "  |  📍 " + m.getLocation() +
                        "  |  Joined: " + SDF.format(new Date(m.getRegisteredAt())));

                h.itemView.findViewById(R.id.btnApprove).setOnClickListener(v -> confirmApprove(m));
                h.itemView.findViewById(R.id.btnSuspend).setOnClickListener(v -> confirmSuspend(m));
            }
        });
    }

    private void confirmApprove(Musician m) {
        new AlertDialog.Builder(this)
                .setTitle("Approve " + m.getFullName() + "?")
                .setMessage("This will activate their account and send them a push notification.")
                .setPositiveButton("✔ Approve", (d, w) ->
                        fb.approveMusician(m.getId(), new FirebaseHelper.OnCompleteListener<Void>() {
                            @Override public void onSuccess(Void r) { toast("Approved ✔"); }
                            @Override public void onFailure(Exception e) { toast("Error: " + e.getMessage()); }
                        }))
                .setNegativeButton("Cancel", null).show();
    }

    private void confirmSuspend(Musician m) {
        new AlertDialog.Builder(this)
                .setTitle("Suspend " + m.getFullName() + "?")
                .setPositiveButton("✘ Suspend", (d, w) ->
                        fb.suspendMusician(m.getId(), new FirebaseHelper.OnCompleteListener<Void>() {
                            @Override public void onSuccess(Void r) { toast("Suspended"); }
                            @Override public void onFailure(Exception e) { toast("Error: " + e.getMessage()); }
                        }))
                .setNegativeButton("Cancel", null).show();
    }

    // ── Pending gig requests ──────────────────────────────────────────────────

    private void loadPendingRequests() {
        fb.getPendingRequests().addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            pendingRequestList.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                GigRequest r = doc.toObject(GigRequest.class);
                if (r != null) pendingRequestList.add(r);
            }
            setupRequestAdapter();
        });
    }

    private void setupRequestAdapter() {
        rvPendingRequests.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public int getItemCount() { return pendingRequestList.size(); }
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_admin_request, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                GigRequest r = pendingRequestList.get(pos);
                ((TextView) h.itemView.findViewById(R.id.tvInfo)).setText(
                        "🎸 Musician: " + r.getMusicianName() + "\n" +
                        "👤 Requester: " + r.getRequesterName() + " (" + r.getRequesterPhone() + ")\n" +
                        "🎉 Event: " + r.getEventName() + "\n" +
                        "📅 Date: " + r.getEventDate() + "  " + (r.getEventTime() != null ? r.getEventTime() : "") + "\n" +
                        "📍 Venue: " + r.getEventLocation() + "\n" +
                        "💰 Amount: TZS " + TZS.format(r.getAmount()) + "  |  Payment: " + r.getPaymentStatus());

                h.itemView.findViewById(R.id.btnApprove).setOnClickListener(v ->
                        updateRequest(r.getId(), r.getMusicianId(), "Approved", "Gig approved by admin."));
                h.itemView.findViewById(R.id.btnReject).setOnClickListener(v ->
                        updateRequest(r.getId(), r.getMusicianId(), "Rejected", "Gig rejected by admin."));
            }
        });
    }

    private void updateRequest(String id, String musicianId, String status, String notes) {
        fb.updateRequestStatus(id, status, notes, new FirebaseHelper.OnCompleteListener<Void>() {
            @Override public void onSuccess(Void r) {
                toast("Request " + status);
                if ("Approved".equals(status)) {
                    fb.sendNotificationToTopic("musician_" + musicianId,
                            "🎉 New Gig Approved!",
                            "A gig request has been approved for you. Check your dashboard!");
                }
            }
            @Override public void onFailure(Exception e) { toast("Error: " + e.getMessage()); }
        });
    }

    // ── Pending payments ──────────────────────────────────────────────────────

    private void loadPendingPayments() {
        fb.getPendingPayments().addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            pendingPaymentList.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Payment p = doc.toObject(Payment.class);
                if (p != null) pendingPaymentList.add(p);
            }
            setupPaymentAdapter();
        });
    }

    private void setupPaymentAdapter() {
        rvPendingPayments.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public int getItemCount() { return pendingPaymentList.size(); }
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_admin_payment, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                Payment p = pendingPaymentList.get(pos);
                String cardInfo = (p.getCardLast4() != null)
                        ? p.getCardBrand() + " ****" + p.getCardLast4()
                        : p.getPaymentMethod();
                ((TextView) h.itemView.findViewById(R.id.tvInfo)).setText(
                        "🎸 Booked: " + p.getMusicianName() + "\n" +
                        "👤 By: " + p.getRequesterName() + "\n" +
                        "💳 Method: " + cardInfo + "\n" +
                        "🔖 Tx ID: " + (p.getClickpesaTransactionId() != null
                                ? p.getClickpesaTransactionId() : "—") + "\n" +
                        "💰 Amount: TZS " + TZS.format(p.getAmount()) + "\n" +
                        "🕐 Paid: " + SDF.format(new Date(p.getPaidAt())));

                h.itemView.findViewById(R.id.btnConfirm).setOnClickListener(v -> confirmPayment(p.getId(), p.getMusicianId()));
                h.itemView.findViewById(R.id.btnReject).setOnClickListener(v  -> rejectPayment(p.getId()));
            }
        });
    }

    private void confirmPayment(String paymentId, String musicianId) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Payment?")
                .setMessage("This will mark the payment as confirmed and notify the musician.")
                .setPositiveButton("✔ Confirm", (d, w) ->
                        fb.confirmPayment(paymentId, fb.getCurrentUser().getUid(),
                                new FirebaseHelper.OnCompleteListener<Void>() {
                                    @Override public void onSuccess(Void r) {
                                        toast("Payment confirmed ✔");
                                        fb.sendNotificationToTopic("musician_" + musicianId,
                                                "💰 Payment Confirmed!",
                                                "Your booking payment has been confirmed. Get ready for the gig!");
                                    }
                                    @Override public void onFailure(Exception e) { toast("Error: " + e.getMessage()); }
                                }))
                .setNegativeButton("Cancel", null).show();
    }

    private void rejectPayment(String paymentId) {
        fb.getDb().collection(FirebaseHelper.COL_PAYMENTS).document(paymentId)
                .update("status", "Rejected")
                .addOnSuccessListener(v -> toast("Payment rejected"))
                .addOnFailureListener(e -> toast("Error: " + e.getMessage()));
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu); return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            fb.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
