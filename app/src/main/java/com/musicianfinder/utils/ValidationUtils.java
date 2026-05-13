package com.musicianfinder.utils;

import android.text.TextUtils;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-Z\\s]{2,50}$");
    private static final Pattern TZ_PHONE_PATTERN =
            Pattern.compile("^(\\+255|0)[67]\\d{8}$");
    private static final Pattern INTL_PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{6,14}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern CARD_EXPIRY_PATTERN =
            Pattern.compile("^(0[1-9]|1[0-2])/(\\d{2}|\\d{4})$");

    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && NAME_PATTERN.matcher(name.trim()).matches();
    }
    public static boolean isValidTanzaniaPhone(String phone) {
        return !TextUtils.isEmpty(phone) && TZ_PHONE_PATTERN.matcher(phone.trim()).matches();
    }
    public static boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        String p = phone.trim();
        return TZ_PHONE_PATTERN.matcher(p).matches() || INTL_PHONE_PATTERN.matcher(p).matches();
    }
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    public static boolean isValidPassword(String pw) {
        if (TextUtils.isEmpty(pw) || pw.length() < 8) return false;
        boolean hasD = false, hasL = false;
        for (char c : pw.toCharArray()) {
            if (Character.isDigit(c))  hasD = true;
            if (Character.isLetter(c)) hasL = true;
        }
        return hasD && hasL;
    }

    /** Luhn algorithm card number check */
    public static boolean isValidCardNumber(String number) {
        String n = number.replaceAll("\\s","");
        if (!n.matches("\\d{13,19}")) return false;
        int sum = 0;
        boolean alternate = false;
        for (int i = n.length() - 1; i >= 0; i--) {
            int d = n.charAt(i) - '0';
            if (alternate) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    public static boolean isValidExpiry(String expiry) {
        if (!CARD_EXPIRY_PATTERN.matcher(expiry).matches()) return false;
        String[] parts = expiry.split("/");
        int month = Integer.parseInt(parts[0]);
        int year  = Integer.parseInt(parts[1].length() == 2 ? "20" + parts[1] : parts[1]);
        java.util.Calendar now = java.util.Calendar.getInstance();
        int curYear  = now.get(java.util.Calendar.YEAR);
        int curMonth = now.get(java.util.Calendar.MONTH) + 1;
        return year > curYear || (year == curYear && month >= curMonth);
    }

    // ── Error helpers ──────────────────────────────────────────────────────────
    public static String getNameError(String n) {
        if (TextUtils.isEmpty(n))   return "Name is required";
        if (!isValidName(n))        return "Name must contain only letters (2–50 chars)";
        return null;
    }
    public static String getPhoneError(String p) {
        if (TextUtils.isEmpty(p))   return "Phone number is required";
        if (!isValidPhone(p))       return "Enter a valid phone number (e.g. +255712345678)";
        return null;
    }
    public static String getEmailError(String e) {
        if (TextUtils.isEmpty(e))   return "Email is required";
        if (!isValidEmail(e))       return "Enter a valid email address";
        return null;
    }
    public static String getPasswordError(String p) {
        if (TextUtils.isEmpty(p))   return "Password is required";
        if (p.length() < 8)         return "Password must be at least 8 characters";
        if (!isValidPassword(p))    return "Password must include letters and numbers";
        return null;
    }
    public static String getCardNumberError(String n) {
        if (TextUtils.isEmpty(n))   return "Card number is required";
        if (!isValidCardNumber(n))  return "Enter a valid card number";
        return null;
    }
    public static String getExpiryError(String e) {
        if (TextUtils.isEmpty(e))   return "Expiry date is required";
        if (!isValidExpiry(e))      return "Card is expired or invalid (use MM/YY)";
        return null;
    }
}
