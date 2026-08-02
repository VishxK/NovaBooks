package lk.vish.novabooks.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PayHereHash {

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 Error", e);
        }
    }

    public static String generateHash(String merchantId, String orderId, String formattedAmount, String currency, String merchantSecret) {
        String secretHash = md5(merchantSecret).toUpperCase();

        String rawSequence = merchantId + orderId + formattedAmount + currency + secretHash;

        return md5(rawSequence).toUpperCase();
    }
}