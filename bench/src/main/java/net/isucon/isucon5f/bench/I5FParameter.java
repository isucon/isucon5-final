package net.isucon.isucon5f.bench;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

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

    public void putObject(String name, Map value) {
        switch (name) {
        case "subscriptions": this.subscriptions = (Map<String,I5FService>) value; break;
        }
    }

    public static class I5FService {
        public String token;
        public List<String> keys;
        public Map<String,String> params;
    }

    private static I5FService dummyServiceKen(Random random) {
        I5FService s = new I5FService();
        s.keys = new ArrayList<String>();
        s.keys.add(I5FZipcodes.list[random.nextInt(I5FZipcodes.list.length)][0]);
        return s;
    }

    private static I5FService dummyServiceKen2(Random random) {
        I5FService s = new I5FService();
        s.params = new HashMap<String,String>();
        s.params.put("zipcode", I5FZipcodes.list[random.nextInt(I5FZipcodes.list.length)][0]);
        return s;
    }

    private static I5FService dummyServiceGivenname(Random random) {
        I5FService s = new I5FService();
        s.params = new HashMap<String,String>();
        //s.params.put("q", I5FGivennames.list[random.nextInt(I5FGivennames.list.length)]);
        return s;
    }

    private static I5FService dummyServiceSurname(Random random) {
        I5FService s = new I5FService();
        s.params = new HashMap<String,String>();
        //s.params.put("q", I5FSurnames.list[random.nextInt(I5FSurnames.list.length)]);
        return s;
    }

    public void putDummySubscriptions() {
        Random rand = new Random();
        int index = rand.nextInt(4) * 3;

        Map<String,I5FService> subs = new HashMap<String,I5FService>();
        switch (grade) {
        case "premium":
            // set premium only service
        case "standard":
            // set standard or premium
        case "small":
            // ...
        case "micro":
            subs.put("ken", dummyServiceKen(rand));
            subs.put("ken2", dummyServiceKen2(rand));
            // "givenname":{"params":{"q":"さと"}}
            // "surname":{"params":{"q":"神"}},}
            break;
        default:
            throw new IllegalArgumentException("Undefined grade:" + grade);
        }
        this.subscriptions = subs;
    }
}
