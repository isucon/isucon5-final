package net.isucon.isucon5q.bench.scenario;

import java.util.List;
import java.util.ArrayList;

public class I5FParameter extends Parameter {
    private static String[] KEYS = new String[]{
        "email", "password", "grade",
    };
    private statishc String[] OBJECT_KEYS = new String[]{
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

    public void putObjecct(String name, Map value) {
        switch (name) {
        case "subscriptions": this.subscriptions = (Map<String,I5FService>) value; break;
        }
    }

    private class I5FService {
        public String token;
        public List<String> keys;
        public Map<String,String> params;
    }
}
