# 🎵 Musician Gig Finder v2.0 — Android App

## What's New in v2.0

### 💳 ClickPesa Payment Gateway
- **Mobile Money:** M-Pesa (Vodacom), Airtel Money, Mixx by Yas (Tigo), HaloPesa, EzyPesa
- **Card Payments:** Visa, Mastercard, UnionPay — encrypted over HTTPS
- Real-time USSD push → user approves on phone → auto-confirmed
- Payment status polling (every 5 seconds, up to 2 minutes)
- All transaction IDs and references saved to Firestore

### 💰 Updated Pricing
| Skill Level | Booking Fee |
|---|---|
| Medium   | TZS 20,000 |
| Advanced | TZS 30,000 |

### 🎹 Advanced Musician Criteria
- Advanced musicians **must** confirm they can play **all 12 musical keys**
- Checkbox enforced at registration — cannot register as Advanced without it
- 12-keys badge shown on musician grid cards and profile page
- Admin sees the badge in the approval list

### 📊 Admin Improvements
- Revenue tracking dashboard (total confirmed TZS)
- Full booking details in payment list: musician name, requester name, method, tx ID
- Approve/reject requests notifies musician via FCM push

---

## ⚙️ Setup

### 1. Firebase
Same as v1.0 — see full procedure in the setup guide DOCX.
- Auth: Email/Password
- Firestore: Production mode
- Storage: Blaze plan required
- FCM: enabled

### 2. ClickPesa API Keys
Open `app/src/main/java/com/musicianfinder/payment/ClickPesaService.java`:

```java
public static final String API_KEY  = "YOUR_CLICKPESA_API_KEY";
public static final String SECRET   = "YOUR_CLICKPESA_SECRET";
public static final String BASE_URL = "https://sandbox.clickpesa.com"; // change to prod
```

**Get your keys:**
1. Register at https://clickpesa.com
2. Go to Dashboard → API Keys
3. Copy API Key and Secret
4. For production: change BASE_URL to `https://api.clickpesa.com`

### 3. Webhook (Server-Side)
For payment confirmation callbacks, set up a webhook endpoint:
- Your server receives POST from ClickPesa when payment completes
- Update Firestore payment document status to "Confirmed"
- Send FCM notification to musician

---

## 🔐 Security
- Card data sent over HTTPS only — never stored on device
- Luhn algorithm validates card numbers before submission
- Regex validation on all input fields
- Proguard obfuscation enabled for release builds
- Network security config enforces HTTPS only

---

## 📱 Payment Flow

```
Requester taps "Request This Musician"
         ↓
Fills gig details → submitted to Firestore
         ↓
PaymentActivity opens (two tabs)
         ↓
[Mobile Money Tab]          [Card Tab]
Select provider             Enter Visa/MC/UnionPay
Enter phone number          Enter card details
         ↓                           ↓
ClickPesa API called        ClickPesa card API
         ↓                           ↓
USSD push to phone          Card charged instantly
User enters PIN
         ↓                           ↓
App polls status every 5s   Confirmed / 3DS pending
         ↓
Payment Confirmed ✅
Firestore updated
Admin sees it in dashboard
Admin taps ✔ Confirm
Musician notified via FCM 🔔
```
