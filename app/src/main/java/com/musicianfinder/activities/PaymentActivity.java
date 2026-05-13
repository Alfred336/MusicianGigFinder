package com.musicianfinder.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.musicianfinder.R;
import com.musicianfinder.models.GigRequest;
import com.musicianfinder.models.Payment;
import com.musicianfinder.payment.ClickPesaService;
import com.musicianfinder.utils.FirebaseHelper;
import com.musicianfinder.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Full ClickPesa payment activity.
 * Two tabs: Mobile Money | Card Payment
 */
public class PaymentActivity extends AppCompatActivity {

    // ── Extras ─────────────────────────────────────────────────────────────────
    public static final String EXTRA_REQUEST_ID    = "requestId";
    public static final String EXTRA_MUSICIAN_ID   = "musicianId";
    public static final String EXTRA_MUSICIAN_NAME = "musicianName";
    public static final String EXTRA_REQUESTER_NAME= "requesterName";
    public static final String EXTRA_AMOUNT        = "amount";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TabLayout          tabLayout;
    // Mobile money tab
    private View               panelMobile;
    private RadioGroup         rgProvider;
    private TextInputEditText  etPhone;
    private MaterialButton     btnPayMobile;
    // Card tab
    private View               panelCard;
    private TextInputEditText  etCardNumber, etExpiry, etCvv, etCardHolder;
    private TextView           tvCardBrand;
    private MaterialButton     btnPayCard;
    // Shared
    private LinearLayout       layoutStatus;
    private TextView           tvStatusTitle, tvStatusMsg;
    private ProgressBar        progressBar;
    private MaterialButton     btnDone;

    // ── State ──────────────────────────────────────────────────────────────────
    private FirebaseHelper     fb;
    private ClickPesaService   clickPesa;
    private Handler            handler  = new Handler(Looper.getMainLooper());
    private Runnable           pollRunnable;
    private String             pendingTxId;
    private int                pollCount = 0;
    private static final int   MAX_POLLS = 24;  // poll for up to 2 minutes

    // Passed in
    private String requestId, musicianId, musicianName, requesterName;
    private int    amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Complete Payment");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        fb        = FirebaseHelper.getInstance();
        clickPesa = new ClickPesaService();

        // Pull extras
        requestId     = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        musicianId    = getIntent().getStringExtra(EXTRA_MUSICIAN_ID);
        musicianName  = getIntent().getStringExtra(EXTRA_MUSICIAN_NAME);
        requesterName = getIntent().getStringExtra(EXTRA_REQUESTER_NAME);
        amount        = getIntent().getIntExtra(EXTRA_AMOUNT, 0);

        bindViews();
        setupTabs();
        setupProviderRadioGroup();
        setupCardNumberWatcher();
        setupExpiryWatcher();
    }

    // ── View binding ───────────────────────────────────────────────────────────
    private void bindViews() {
        tabLayout    = findViewById(R.id.tabLayout);
        panelMobile  = findViewById(R.id.panelMobile);
        panelCard    = findViewById(R.id.panelCard);
        rgProvider   = findViewById(R.id.rgProvider);
        etPhone      = findViewById(R.id.etPhone);
        btnPayMobile = findViewById(R.id.btnPayMobile);
        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiry     = findViewById(R.id.etExpiry);
        etCvv        = findViewById(R.id.etCvv);
        etCardHolder = findViewById(R.id.etCardHolder);
        tvCardBrand  = findViewById(R.id.tvCardBrand);
        btnPayCard   = findViewById(R.id.btnPayCard);
        layoutStatus = findViewById(R.id.layoutStatus);
        tvStatusTitle= findViewById(R.id.tvStatusTitle);
        tvStatusMsg  = findViewById(R.id.tvStatusMsg);
        progressBar  = findViewById(R.id.progressBar);
        btnDone      = findViewById(R.id.btnDone);

        // Summary
        ((TextView) findViewById(R.id.tvPaymentSummary)).setText(
                "Booking: " + musicianName +
                "\nAmount : TZS " + String.format("%,d", amount) +
                "\nRequest: " + requestId);

        btnPayMobile.setOnClickListener(v -> processMobilePayment());
        btnPayCard.setOnClickListener(v  -> processCardPayment());
        btnDone.setOnClickListener(v    -> finish());
    }

    // ── Tabs ───────────────────────────────────────────────────────────────────
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("📱 Mobile Money"));
        tabLayout.addTab(tabLayout.newTab().setText("💳 Card Payment"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                panelMobile.setVisibility(tab.getPosition() == 0 ? View.VISIBLE : View.GONE);
                panelCard.setVisibility(tab.getPosition()   == 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── Provider selection ─────────────────────────────────────────────────────
    private void setupProviderRadioGroup() {
        // Dynamic provider prefix per selection
        rgProvider.setOnCheckedChangeListener((g, checkedId) -> {
            String hint = getPhoneHint(checkedId);
            etPhone.setHint(hint);
        });
    }

    private String getPhoneHint(int radioId) {
        if (radioId == R.id.rbMpesa)      return "M-Pesa number (e.g. 0712345678)";
        if (radioId == R.id.rbAirtel)     return "Airtel number (e.g. 0682345678)";
        if (radioId == R.id.rbMixx)       return "Tigo/Mixx number (e.g. 0652345678)";
        if (radioId == R.id.rbHaloPesa)   return "Halotel number (e.g. 0622345678)";
        if (radioId == R.id.rbEzyPesa)    return "TTCL/EzyPesa number";
        return "Phone number";
    }

    private String getSelectedProvider() {
        int id = rgProvider.getCheckedRadioButtonId();
        if (id == R.id.rbMpesa)    return Payment.CP_VODACOM;
        if (id == R.id.rbAirtel)   return Payment.CP_AIRTEL;
        if (id == R.id.rbMixx)     return Payment.CP_TIGO;
        if (id == R.id.rbHaloPesa) return Payment.CP_HALOTEL;
        if (id == R.id.rbEzyPesa)  return Payment.CP_TNMPESA;
        return null;
    }

    private String getSelectedMethodName() {
        int id = rgProvider.getCheckedRadioButtonId();
        if (id == R.id.rbMpesa)    return Payment.METHOD_MPESA;
        if (id == R.id.rbAirtel)   return Payment.METHOD_AIRTEL;
        if (id == R.id.rbMixx)     return Payment.METHOD_MIXX;
        if (id == R.id.rbHaloPesa) return Payment.METHOD_HALOPESA;
        if (id == R.id.rbEzyPesa)  return Payment.METHOD_EZYPESA;
        return "Mobile Money";
    }

    // ── Mobile money payment ───────────────────────────────────────────────────
    private void processMobilePayment() {
        String phone    = etPhone.getText().toString().trim();
        String provider = getSelectedProvider();

        if (provider == null) { toast("Please select a payment provider"); return; }
        if (!ValidationUtils.isValidTanzaniaPhone(phone)) {
            etPhone.setError("Enter a valid Tanzanian phone number"); return;
        }

        setLoading(true);
        showStatus("⏳ Sending USSD push…",
                "A payment prompt will appear on your phone.\nEnter your PIN to approve.",
                Color.parseColor("#1565C0"), false);

        String description = "Gig booking: " + musicianName + " | Ref: " + requestId;

        clickPesa.requestMobileMoneyPayment(phone, amount, provider, requestId, description,
                new ClickPesaService.PaymentCallback() {
                    @Override public void onSuccess(String txId, String msg) {
                        runOnUiThread(() -> {
                            pendingTxId = txId;
                            startPolling(txId);
                        });
                    }
                    @Override public void onPending(String txId, String msg) {
                        runOnUiThread(() -> {
                            pendingTxId = txId;
                            showStatus("📲 USSD Push Sent!", msg + "\n\nWaiting for PIN confirmation…",
                                    Color.parseColor("#1565C0"), true);
                            startPolling(txId);
                        });
                    }
                    @Override public void onFailure(String error) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showStatus("❌ Payment Failed", error, Color.RED, false);
                        });
                    }
                });
    }

    // ── Card payment ───────────────────────────────────────────────────────────
    private void processCardPayment() {
        String cardNumber = etCardNumber.getText().toString().replaceAll("\\s", "");
        String expiry     = etExpiry.getText().toString().trim();
        String cvv        = etCvv.getText().toString().trim();
        String holder     = etCardHolder.getText().toString().trim();

        if (!validateCard(cardNumber, expiry, cvv, holder)) return;

        String[] exp = expiry.split("/");
        String month = exp[0].trim();
        String year  = (exp[1].trim().length() == 2) ? "20" + exp[1].trim() : exp[1].trim();

        String brand = ClickPesaService.detectCardBrand(cardNumber);

        setLoading(true);
        showStatus("⏳ Processing card…",
                "Securely charging your " + brand + " card.\nPlease wait…",
                Color.parseColor("#1565C0"), false);

        clickPesa.requestCardPayment(cardNumber, month, year, cvv, holder,
                amount, requestId, "Gig booking: " + musicianName,
                new ClickPesaService.PaymentCallback() {
                    @Override public void onSuccess(String txId, String msg) {
                        runOnUiThread(() -> onPaymentConfirmed(txId, brand + " ****" + cardNumber.substring(cardNumber.length()-4), brand, cardNumber.substring(cardNumber.length()-4)));
                    }
                    @Override public void onPending(String txId, String msg) {
                        runOnUiThread(() -> {
                            pendingTxId = txId;
                            showStatus("🔐 3D-Secure", msg, Color.parseColor("#1565C0"), true);
                            startPolling(txId);
                        });
                    }
                    @Override public void onFailure(String error) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showStatus("❌ Card Declined", error, Color.RED, false);
                        });
                    }
                });
    }

    private boolean validateCard(String num, String exp, String cvv, String holder) {
        if (num.length() < 13 || num.length() > 19) { etCardNumber.setError("Invalid card number"); return false; }
        if (!exp.matches("\\d{2}/\\d{2,4}"))         { etExpiry.setError("Use MM/YY format");       return false; }
        if (cvv.length() < 3 || cvv.length() > 4)   { etCvv.setError("Invalid CVV");               return false; }
        if (holder.trim().isEmpty())                  { etCardHolder.setError("Enter card holder name"); return false; }
        return true;
    }

    // ── Status polling ─────────────────────────────────────────────────────────
    private void startPolling(String txId) {
        pollCount = 0;
        pollRunnable = new Runnable() {
            @Override public void run() {
                if (pollCount >= MAX_POLLS) {
                    setLoading(false);
                    showStatus("⏰ Timeout",
                            "Payment not confirmed yet. Contact admin with reference:\n" + txId,
                            Color.parseColor("#E65100"), false);
                    savePaymentPending(txId, null, null);
                    return;
                }
                pollCount++;
                clickPesa.checkPaymentStatus(txId, new ClickPesaService.StatusCallback() {
                    @Override public void onConfirmed(String tx, String ref) {
                        runOnUiThread(() -> onPaymentConfirmed(tx, ref,
                                getSelectedMethodName(), null));
                    }
                    @Override public void onPending(String tx) {
                        runOnUiThread(() -> showStatus("⏳ Waiting for PIN…",
                                "Polls: " + pollCount + "/" + MAX_POLLS +
                                "\nPlease approve on your phone.",
                                Color.parseColor("#1565C0"), true));
                        handler.postDelayed(pollRunnable, 5_000);
                    }
                    @Override public void onFailed(String tx, String reason) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showStatus("❌ Payment Failed", reason, Color.RED, false);
                        });
                    }
                    @Override public void onError(String err) {
                        // network glitch — retry
                        handler.postDelayed(pollRunnable, 5_000);
                    }
                });
            }
        };
        handler.postDelayed(pollRunnable, 5_000);
    }

    // ── Payment confirmed ──────────────────────────────────────────────────────
    private void onPaymentConfirmed(String txId, String reference, String method, String cardLast4) {
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
        setLoading(false);

        // Save to Firestore
        savePaymentConfirmed(txId, reference, method, cardLast4);

        showStatus("✅ Payment Confirmed!",
                "Amount  : TZS " + String.format("%,d", amount) +
                "\nMethod  : " + method +
                "\nRef     : " + (reference != null ? reference : txId) +
                "\nMusician: " + musicianName +
                "\n\nAdmin has been notified. They will approve your gig shortly.",
                Color.parseColor("#1B5E20"), false);
        btnDone.setVisibility(View.VISIBLE);
    }

    // ── Firestore saves ────────────────────────────────────────────────────────
    private void savePaymentConfirmed(String txId, String ref, String method, String cardLast4) {
        Map<String, Object> data = new HashMap<>();
        data.put("gigRequestId",             requestId);
        data.put("musicianId",               musicianId);
        data.put("musicianName",             musicianName);
        data.put("requesterName",            requesterName);
        data.put("amount",                   amount);
        data.put("paymentMethod",            method);
        data.put("clickpesaTransactionId",   txId);
        data.put("referenceNumber",          ref != null ? ref : txId);
        data.put("status",                   Payment.STATUS_CONFIRMED);
        data.put("paidAt",                   System.currentTimeMillis());
        if (cardLast4 != null) {
            data.put("cardLast4", cardLast4);
            data.put("cardBrand", method);
        }
        fb.getDb().collection(FirebaseHelper.COL_PAYMENTS).add(data)
                .addOnSuccessListener(ref2 -> {
                    // Also update gig request payment status
                    fb.getDb().collection(FirebaseHelper.COL_REQUESTS).document(requestId)
                            .update("paymentStatus", Payment.STATUS_CONFIRMED,
                                    "paymentReference", txId);
                });
    }

    private void savePaymentPending(String txId, String method, String cardLast4) {
        Map<String, Object> data = new HashMap<>();
        data.put("gigRequestId",           requestId);
        data.put("musicianId",             musicianId);
        data.put("musicianName",           musicianName);
        data.put("requesterName",          requesterName);
        data.put("amount",                 amount);
        data.put("paymentMethod",          method != null ? method : getSelectedMethodName());
        data.put("clickpesaTransactionId", txId);
        data.put("status",                 Payment.STATUS_PENDING);
        data.put("paidAt",                 System.currentTimeMillis());
        fb.getDb().collection(FirebaseHelper.COL_PAYMENTS).add(data);
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────
    private void showStatus(String title, String msg, int color, boolean showProgress) {
        layoutStatus.setVisibility(View.VISIBLE);
        tvStatusTitle.setText(title);
        tvStatusTitle.setTextColor(color);
        tvStatusMsg.setText(msg);
        progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }

    private void setLoading(boolean loading) {
        btnPayMobile.setEnabled(!loading);
        btnPayCard.setEnabled(!loading);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    // ── Card number formatting & brand detection ───────────────────────────────
    private void setupCardNumberWatcher() {
        etCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean editing = false;
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){}
            @Override public void afterTextChanged(Editable s) {
                if (editing) return;
                editing = true;
                String digits = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length() && i < 16; i++) {
                    if (i > 0 && i % 4 == 0) formatted.append(' ');
                    formatted.append(digits.charAt(i));
                }
                etCardNumber.setText(formatted);
                etCardNumber.setSelection(formatted.length());
                // Update brand label
                String brand = ClickPesaService.detectCardBrand(digits);
                tvCardBrand.setText(brand);
                editing = false;
            }
        });
    }

    private void setupExpiryWatcher() {
        etExpiry.addTextChangedListener(new TextWatcher() {
            private boolean editing = false;
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){}
            @Override public void afterTextChanged(Editable s) {
                if (editing) return;
                editing = true;
                String raw = s.toString().replaceAll("[^\\d]", "");
                if (raw.length() >= 3) {
                    raw = raw.substring(0,2) + "/" + raw.substring(2, Math.min(raw.length(),4));
                }
                etExpiry.setText(raw);
                etExpiry.setSelection(raw.length());
                editing = false;
            }
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
