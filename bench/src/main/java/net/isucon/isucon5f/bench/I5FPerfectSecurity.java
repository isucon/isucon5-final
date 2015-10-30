package net.isucon.isucon5f.bench;

import java.util.Random;
import java.util.Arrays;
import java.util.Date;

import java.security.MessageDigest;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.DatatypeConverter;

public class I5FPerfectSecurity {
    public static long VALID_CACHE_MILLIS = 16000; // 15s + 1

    private static String TOKEN_SEED = "tony";
    private static int TOKEN_RAND_MAX = 1000000;

    private static String ONETIME_SECRET = "yeeeeeah! miteru-? yeeeeeah! miteru-! yeeeeeah! miteru-?";
    private static String ATTACKED_SECRET = "toooooonyyyyyy";
    private static String[] ATTACKED_SEQ = new String[]{ "001", "002", "003" };

    private static String[] list = new String[]{
        "perfect", "ultimate", "exorbitant", "extreme", "supreme", "abnormal", "magnificent", "unforgettable",
    };

    private static MessageDigest getDigest(String type) {
        try {
            return MessageDigest.getInstance(type);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm: " + type);
        }
    }

    public static boolean isCorrectOneTime(String onetime, String token, String req, String key, long responseAt) {
        // var onetime_token = create_onetime_token(token + ' ' + key + ' ' + req + ' ' + (new Date().getTime()));
        try {
            SecretKey cipherKey = new SecretKeySpec(getDigest("MD5").digest(ONETIME_SECRET.getBytes("UTF-8")), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, cipherKey);
            String content = new String(cipher.doFinal(DatatypeConverter.parseHexBinary(onetime)), "utf-8");

            String[] parts = content.split(" +", 4);
            if (parts.length != 4)
                return false;

            long contentAt = Long.parseLong(parts[3]);
            return token.equals(parts[0]) && key.equals(parts[1]) && req.equals(parts[2]) && (responseAt - contentAt) <= VALID_CACHE_MILLIS;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            System.err.println("Decryption error " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Decryption error: " + e.getClass().getName());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Decryption exception for data " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static String[] getAttacked(String token, String epoch) {
        String[] attacked_tokens = new String[3];
        for (int i = 0 ; i < 3 ; i++) {
            MessageDigest md = getDigest("SHA-1");
            // crypto.createHash('sha1').update(token + '001' + epoch + 'toooooonyyyyyy').digest('hex')
            md.update((token + ATTACKED_SEQ[i] + epoch + ATTACKED_SECRET).getBytes());
            attacked_tokens[i] = DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
        }
        return attacked_tokens;
    }

    public static String getToken(Random random) {
        MessageDigest md = getDigest("SHA-1");
        md.update((TOKEN_SEED + String.valueOf(random.nextInt(TOKEN_RAND_MAX))).getBytes());
        return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
    }

    public static String getReq(Random random) {
        return list[random.nextInt(list.length)];
    }
}
