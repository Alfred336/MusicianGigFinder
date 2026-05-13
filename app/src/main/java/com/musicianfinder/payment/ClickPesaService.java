package com.musicianfinder.payment;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ClickPesa Payment Gateway Integration
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  SETUP (replace placeholders in BuildConfig or strings.xml):
 *   • CLICKPESA_API_KEY   = your ClickPesa API key
 *   • CLICKPESA_SECRET    = your ClickPesa secret
 *   • CLICKPESA_BASE_URL  = https://api.clickpesa.com  (production)
 *                          https://sandbox.clickpesa.com (testing)
 *
 *  Supported mobile providers:
 *   VODACOM (M-Pesa), AIRTEL (Airtel Money), TIGO (Mixx by Yas),
 *   HALOTEL (HaloPesa), TNMPESA (EzyPesa)
 *
 *  Card payments: Visa, Mastercard, UnionPay via ClickPesa card API
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ClickPesaService {

    private static final String TAG = "ClickPesaService";

    // ── Replace with your real keys (or pull from BuildConfig / secured storage) ──
    public static final String API_KEY  = "YOUR_CLICKPESA_API_KEY";
    public static final String SECRET   = "YOUR_CLICKPESA_SECRET";
    public static final String BASE_URL = "https://sandbox.clickpesa.com"; // switch to prod

    // ClickPesa API endpoints
    private static final String ENDPOINT_MOBILE_PAY = "/api/v3/orders/request-payment";
    private static final String ENDPOINT_CARD_PAY   = "/api/v3/orders/charge-card";
    private static final String ENDPOINT_STATUS     = "/api/v3/orders/payment-status/";
    private static final String ENDPOINT_TOKEN      = "/api/v3/clients/generate-token";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ── Callback interfaces ────────────────────────────────────────────────────
    public interface PaymentCallback {
        void onSuccess(String transactionId, String message);
        void onPending(String transactionId, String message);
        void onFailure(String errorMessage);
    }

    public interface StatusCallback {
        void onConfirmed(String transactionId, String reference);
        void onPending(String transactionId);
        void onFailed(String transactionId, String reason);
        void onError(String errorMessage);
    }

    // ── Authentication token cache ─────────────────────────────────────────────
    private String cachedToken   = null;
    private long   tokenExpiry   = 0;

    // ── Main API methods ───────────────────────────────────────────────────────

    /**
     * Initiates a mobile money (USSD Push) payment.
     * Provider codes: VODACOM | AIRTEL | TIGO | HALOTEL | TNMPESA
     */
    public void requestMobileMoneyPayment(
            String phone,
            int    amount,
            String provider,       // e.g. "VODACOM"
            String orderId,        // your internal gig request ID
            String description,
            PaymentCallback callback) {

        executor.execute(() -> {
            try {
                String token = getAuthToken();
                if (token == null) { callback.onFailure("Authentication failed"); return; }

                // Normalize phone number to Tanzanian format
                String normalizedPhone = normalizePhone(phone);

                JSONObject body = new JSONObject();
                body.put("phone_number",     normalizedPhone);
                body.put("amount",           amount);
                body.put("currency",         "TZS");
                body.put("payment_provider", provider);
                body.put("order_id",         orderId);
                body.put("description",      description);
                body.put("callback_url",     "https://your-server.com/payment-webhook"); // set your webhook

                JSONObject response = postJson(ENDPOINT_MOBILE_PAY, body.toString(), token);
                if (response == null) { callback.onFailure("Network error. Check your connection."); return; }

                String status = response.optString("status", "");
                String txId   = response.optString("transaction_id", orderId);
                String msg    = response.optString("message", "Payment request sent");

                if ("success".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) {
                    // USSD push was sent — payment is pending user PIN entry
                    callback.onPending(txId, "USSD push sent to " + normalizedPhone + ". Enter your PIN to approve.");
                } else {
                    callback.onFailure(response.optString("message", "Payment initiation failed"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Mobile payment error", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Initiates a card payment (Visa / Mastercard / UnionPay).
     * Card data is sent encrypted over HTTPS — never stored locally.
     */
    public void requestCardPayment(
            String cardNumber,     // full PAN (sent securely, NOT stored)
            String expiryMonth,
            String expiryYear,
            String cvv,
            String cardHolderName,
            int    amount,
            String orderId,
            String description,
            PaymentCallback callback) {

        executor.execute(() -> {
            try {
                String token = getAuthToken();
                if (token == null) { callback.onFailure("Authentication failed"); return; }

                JSONObject body = new JSONObject();
                body.put("card_number",      cardNumber.replaceAll("\\s", ""));
                body.put("expiry_month",     expiryMonth);
                body.put("expiry_year",      expiryYear);
                body.put("cvv",              cvv);
                body.put("card_holder_name", cardHolderName);
                body.put("amount",           amount);
                body.put("currency",         "TZS");
                body.put("order_id",         orderId);
                body.put("description",      description);
                body.put("callback_url",     "https://your-server.com/payment-webhook");

                JSONObject response = postJson(ENDPOINT_CARD_PAY, body.toString(), token);
                if (response == null) { callback.onFailure("Network error."); return; }

                String status = response.optString("status", "");
                String txId   = response.optString("transaction_id", orderId);

                if ("success".equalsIgnoreCase(status)) {
                    callback.onSuccess(txId, "Card charged successfully!");
                } else if ("pending".equalsIgnoreCase(status)) {
                    callback.onPending(txId, "Card payment pending 3D-Secure verification.");
                } else {
                    callback.onFailure(response.optString("message", "Card payment failed"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Card payment error", e);
                callback.onFailure("Card error: " + e.getMessage());
            }
        });
    }

    /**
     * Poll payment status. Call this every 5 seconds after initiating mobile payment.
     */
    public void checkPaymentStatus(String transactionId, StatusCallback callback) {
        executor.execute(() -> {
            try {
                String token = getAuthToken();
                if (token == null) { callback.onError("Auth failed"); return; }

                JSONObject response = getJson(ENDPOINT_STATUS + transactionId, token);
                if (response == null) { callback.onError("Network error"); return; }

                String status = response.optString("status", "").toLowerCase();
                String ref    = response.optString("reference_number", "");

                switch (status) {
                    case "success":
                    case "completed":
                        callback.onConfirmed(transactionId, ref);
                        break;
                    case "pending":
                    case "processing":
                        callback.onPending(transactionId);
                        break;
                    default:
                        callback.onFailed(transactionId, response.optString("message", "Payment failed"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Status check error", e);
                callback.onError(e.getMessage());
            }
        });
    }

    // ── Auth token ─────────────────────────────────────────────────────────────

    private synchronized String getAuthToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiry) {
            return cachedToken;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("api_key", API_KEY);
            body.put("secret",  SECRET);

            JSONObject response = postJson(ENDPOINT_TOKEN, body.toString(), null);
            if (response == null) return null;

            cachedToken  = response.optString("token", null);
            int expiresIn = response.optInt("expires_in", 3600);
            tokenExpiry  = System.currentTimeMillis() + (expiresIn - 60) * 1000L;
            return cachedToken;

        } catch (Exception e) {
            Log.e(TAG, "Token error", e);
            return null;
        }
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────────

    private JSONObject postJson(String endpoint, String jsonBody, String bearerToken) {
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (bearerToken != null)
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            return readResponse(conn);
        } catch (Exception e) {
            Log.e(TAG, "POST error: " + endpoint, e);
            return null;
        }
    }

    private JSONObject getJson(String endpoint, String bearerToken) {
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);
            return readResponse(conn);
        } catch (Exception e) {
            Log.e(TAG, "GET error: " + endpoint, e);
            return null;
        }
    }

    private JSONObject readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        Log.d(TAG, "Response [" + code + "]: " + sb);
        return new JSONObject(sb.toString());
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    /** Normalize Tanzanian phone numbers to +255XXXXXXXXX */
    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        phone = phone.trim().replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("0") && phone.length() == 10)
            phone = "+255" + phone.substring(1);
        if (!phone.startsWith("+"))
            phone = "+" + phone;
        return phone;
    }

    /** Detect card brand from first digits */
    public static String detectCardBrand(String number) {
        String n = number.replaceAll("\\s", "");
        if (n.startsWith("4"))                       return "Visa";
        if (n.matches("^5[1-5].*"))                  return "Mastercard";
        if (n.matches("^2[2-7].*"))                  return "Mastercard";
        if (n.matches("^(62|88).*"))                 return "UnionPay";
        if (n.matches("^3[47].*"))                   return "AmEx";
        return "Card";
    }

    /** Map display method name to ClickPesa provider code */
    public static String toProviderCode(String displayMethod) {
        switch (displayMethod) {
            case Payment.METHOD_MPESA:    return Payment.CP_VODACOM;
            case Payment.METHOD_AIRTEL:   return Payment.CP_AIRTEL;
            case Payment.METHOD_MIXX:     return Payment.CP_TIGO;
            case Payment.METHOD_HALOPESA: return Payment.CP_HALOTEL;
            case Payment.METHOD_EZYPESA:  return Payment.CP_TNMPESA;
            default:                      return Payment.CP_CARD;
        }
    }

    /** Placeholder — see models/Payment.java for imports */
    private static class Payment {
        static final String METHOD_MPESA    = "M-Pesa (Vodacom)";
        static final String METHOD_AIRTEL   = "Airtel Money";
        static final String METHOD_MIXX     = "Mixx by Yas";
        static final String METHOD_HALOPESA = "HaloPesa";
        static final String METHOD_EZYPESA  = "EzyPesa";
        static final String CP_VODACOM  = "VODACOM";
        static final String CP_AIRTEL   = "AIRTEL";
        static final String CP_TIGO     = "TIGO";
        static final String CP_HALOTEL  = "HALOTEL";
        static final String CP_TNMPESA  = "TNMPESA";
        static final String CP_CARD     = "CARD";
    }
}
