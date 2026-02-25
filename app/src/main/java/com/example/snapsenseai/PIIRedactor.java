package com.example.snapsenseai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PIIRedactor {

    // -------- Government IDs (Fuzzy Patterns for OCR errors) --------
    private static final String AADHAAR = "\\b\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}\\b";
    private static final String PAN = "\\b[A-Z]{5}\\d{4}[A-Z]\\b";
    private static final String GSTIN = "\\b\\d{2}[A-Z]{5}\\d{4}[A-Z][A-Z\\d]{3}\\b";
    private static final String VEHICLE = "\\b[A-Z]{2}\\d{1,2}[A-Z]{0,3}\\d{4}\\b";

    // -------- Financial & UPI (Expanded for Indian Ecosystem) --------
    private static final String CARD = "\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b";
    private static final String IFSC = "\\b[A-Z]{4}0[A-Z0-9]{6}\\b";
    private static final String UPI = "\\b[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9.\\-_]{2,64}\\b";
    private static final String ACCOUNT_CTX = "(?i)(acct|a/c|account|bank)[^0-9]{0,12}(\\d{8,18})";
    private static final String UTR_CTX = "(?i)(UTR|Txn|Ref|ID)[^A-Za-z0-9]{0,10}([A-Za-z0-9]{8,22})";

    // -------- Contact & Auth (Refined for formats like +9199412 96352) --------
    // New pattern: Matches optional +91, followed by 10 digits that can have a space/hyphen anywhere.
    private static final String PHONE_FUZZY = "\\b(?:\\+91|91|0)?[\\s\\-]?[6789]\\d{1,4}[\\s\\-]?\\d{5,8}\\b";

    // Context-Aware Phone Redaction
    private static final String PHONE_CTX = "(?i)(call|mob|ph|contact|tel|to|from)[^0-9]{0,10}(\\b(?:\\+91|91|0)?[\\s\\-]?[6789][\\d\\s\\-]{7,13}\\b)";

    private static final String EMAIL = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
    private static final String OTP_CTX = "(?i)(OTP|code|verify|pin|secure)[^0-9]{0,10}(\\b\\d{4,8}\\b)";

    // -------- Personal & Device --------
    private static final String DOB_CTX = "(?i)(DOB|Birth|Born)[^0-9]{0,10}(\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b)";
    private static final String PINCODE_CTX = "(?i)(pin|pincode|zip)[^0-9]{0,5}(\\b[1-9]\\d{5}\\b)";
    private static final String IP = "\\b(\\d{1,3}\\.){3}\\d{1,3}\\b";

    public String redact(String text) {
        if (text == null || text.isEmpty()) return "";

        String t = text;

        // 1. Direct Redaction (Total Privacy)
        t = t.replaceAll(AADHAAR, "[REDACTED_AADHAAR]");
        t = t.replaceAll(PAN, "[REDACTED_PAN]");
        t = t.replaceAll(GSTIN, "[REDACTED_GSTIN]");
        t = t.replaceAll(VEHICLE, "[REDACTED_VEHICLE]");
        t = t.replaceAll(CARD, "[REDACTED_CARD]");
        t = t.replaceAll(IFSC, "[REDACTED_IFSC]");
        t = t.replaceAll(UPI, "[REDACTED_UPI]");
        t = t.replaceAll(PHONE_FUZZY, "[REDACTED_PHONE]");
        t = t.replaceAll(EMAIL, "[REDACTED_EMAIL]");
        t = t.replaceAll(IP, "[REDACTED_IP]");

        // 2. Context-Aware Redaction (Preserves labels for Phase 3 LLM Analysis)
        t = redactWithLabel(t, ACCOUNT_CTX, "ACCOUNT_NO");
        t = redactWithLabel(t, UTR_CTX, "TRANSACTION_ID");
        t = redactWithLabel(t, OTP_CTX, "OTP_CODE"); // Adjusted label naming
        t = redactWithLabel(t, DOB_CTX, "BIRTH_DATE");
        t = redactWithLabel(t, PINCODE_CTX, "LOCATION_PIN");
        t = redactWithLabel(t, PHONE_CTX, "PHONE_NUMBER");

        return t;
    }

    private String redactWithLabel(String text, String regex, String label) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            // Preserves the keyword (Group 1) and redacts the value (Group 2)
            m.appendReplacement(sb, m.group(1) + ": [REDACTED_" + label + "]");
        }
        m.appendTail(sb);
        return sb.toString();
    }
}