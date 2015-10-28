package net.isucon.isucon5f.bench;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;

import net.isucon.bench.Parameter;

public class I5FParameter extends Parameter {
    private static String[] KEYS = new String[]{
        "email", "password", "grade",
    };
    private static String[] OBJECT_KEYS = new String[]{
         "subscriptions",
    };

    public String email;
    public String password;
    public String grade;
    public Map<String,I5FService> subscriptions;

    public String[] keys() {
        return KEYS;
    }
    public String[] objects() {
        return OBJECT_KEYS;
    }

    public void put(String name, String value) {
        switch (name) {
        case "email":    this.email = value; break;
        case "password": this.password = value; break;
        case "grade":    this.grade = value; break;
        }
    }

    public void putObject(String name, JsonNode node) {
        switch (name) {
        case "subscriptions":
            Map<String,I5FService> subs = new HashMap<String,I5FService>();
            for (Iterator<String> iter = node.fieldNames(); iter.hasNext(); ) {
                String serviceName = iter.next();
                subs.put(serviceName, I5FService.getInstance(node.get(serviceName)));
            }
            this.subscriptions = subs;
            break;
        }
    }

    public static class I5FService {
        public String token;
        public List<String> keys;
        public Map<String,String> params;

        public static I5FService getInstance(JsonNode node) {
            I5FService s = new I5FService();

            JsonNode tokenValue = node.get("token");
            if (tokenValue != null)
                s.token = tokenValue.asText();

            JsonNode keysValue = node.get("keys");
            if (keysValue != null && keysValue.isArray()) {
                ArrayList<String> ks = new ArrayList<String>();
                for (Iterator<JsonNode> iter = keysValue.elements(); iter.hasNext(); ) {
                    ks.add(iter.next().asText());
                }
                s.keys = ks;
            }

            JsonNode paramsValue = node.get("params");
            if (paramsValue != null && paramsValue.isObject()) {
                Map<String,String> ps = new HashMap<String,String>();
                for (Iterator<String> iter = paramsValue.fieldNames(); iter.hasNext(); ) {
                    String k = iter.next();
                    ps.put(k, paramsValue.get(k).asText());
                }
                s.params = ps;
            }

            return s;
        }
    }

    private static I5FService dummyServiceKen(Random random) {
        I5FService s = new I5FService();
        s.keys = new ArrayList<String>();
        s.keys.add(I5FZipcodes.getKey());
        return s;
    }

    private static I5FService dummyServiceKen2(Random random) {
        I5FService s = new I5FService();
        s.params = new HashMap<String,String>();
        s.params.put("zipcode", I5FZipcodes.getKey());
        return s;
    }

    private static I5FService dummyServiceGivenname(Random random) {
        I5FService s = new I5FService();
        s.params = new HashMap<String,String>();
        s.params.put("q", I5FGivennames.getKey());
        return s;
    }

    private static I5FService dummyServiceSurname(Random random) {
        I5FService s = new I5FService();
        s.params = new HashMap<String,String>();
        s.params.put("q", I5FSurnames.getKey());
        return s;
    }

    private static I5FService dummyServiceTenki(Random random) {
        I5FService s = new I5FService();
        s.token = I5FTenki.zipRandom(random);
        return s;
    }

    private static I5FService dummyServicePerfectSecurity(Random random) {
        I5FService s = new I5FService();
        s.params = new HashMap<String,String>();
        s.params.put("req", I5FPerfectSecurity.getReq(random));
        s.token = I5FPerfectSecurity.getToken(random);
        return s;
    }

    private static I5FService dummyServicePerfectSecurityAttacked(Random random) {
        I5FService s = new I5FService();
        return s;
    }

    public void putDummySubscriptions() {
        Random rand = new Random();
        int index = rand.nextInt(4) * 3;

        Map<String,I5FService> subs = new HashMap<String,I5FService>();
        switch (grade) {
        case "premium":
            subs.put("perfectsec", dummyServicePerfectSecurity(rand));
            subs.put("perfectsec_attacked", dummyServicePerfectSecurityAttacked(rand));
        case "standard":
            subs.put("tenki", dummyServiceTenki(rand));
        case "small":
            subs.put("givenname", dummyServiceGivenname(rand));
        case "micro":
            subs.put("ken", dummyServiceKen(rand));
            subs.put("ken2", dummyServiceKen2(rand));
            subs.put("surname", dummyServiceSurname(rand));
            break;
        default:
            throw new IllegalArgumentException("Undefined grade:" + grade);
        }
        this.subscriptions = subs;
    }
}
