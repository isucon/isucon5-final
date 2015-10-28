package net.isucon.isucon5f.bench;

import java.util.Random;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class I5FPerfectSecurity {
    private static String TOKEN_SEED = "tony";
    private static int TOKEN_RAND_MAX = 1000000;

    private static String ONETIME_SECRET = "secret word tony-morris";
    private static String ATTACKED_SECRET = "toooooonyyyyyy";
    private static String[] ATTACKED_SEQ = new String[]{ "001", "002", "003" };

    private static String[] list = new String[]{
        "perfect", "ultimate", "exorbitant", "extreme", "supreme", "abnormal", "magnificent", "unforgettable",
    };

    private static MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm: SHA-1");
        }
    }

    public static String getOneTime(String token, String req, String key) {
        // crypto.createHash('sha1').update(token + 'secret word tony-morris' + key + req).digest('hex');
        MessageDigest md = getDigest();
        md.update((token + ONETIME_SECRET + key + req).getBytes());
        return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
    }

    public static String[] getAttacked(String token, String epoch) {
        String[] attacked_tokens = new String[3];
        for (int i = 0 ; i < 3 ; i++) {
            MessageDigest md = getDigest();
            // crypto.createHash('sha1').update(token + '001' + epoch + 'toooooonyyyyyy').digest('hex')
            md.update((token + ATTACKED_SEQ[i] + epoch + ATTACKED_SECRET).getBytes());
            attacked_tokens[i] = DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
        }
        return attacked_tokens;
    }

    public static String getToken(Random random) {
        MessageDigest md = getDigest();
        md.update((TOKEN_SEED + String.valueOf(random.nextInt(TOKEN_RAND_MAX))).getBytes());
        return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
    }

    public static String getReq(Random random) {
        return list[random.nextInt(list.length)];
    }
}
