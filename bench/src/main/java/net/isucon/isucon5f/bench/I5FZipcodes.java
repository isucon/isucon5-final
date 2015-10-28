package net.isucon.isucon5f.bench;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.core.type.TypeReference;

public class I5FZipcodes {
    private static String SOURCE_DIR_ENV_NAME = "ISUCON_BENCH_DATADIR";
    private static String SOURCE_FILE_BASENAME = "ken_all.json";

    public static Map<String,List<String>> map;
    public static List<String> shuffled;
    private static int pos = 0;

    public static synchronized String getKey() {
        load();
        String key = shuffled.get(pos);
        pos += 1;
        if (pos >= shuffled.size()) {
            pos = 0;
        }
        return key;
    }

    public static String address(String zipcode) {
        load();
        List<String> addresses = map.get(zipcode);
        if (addresses == null)
            throw new IllegalArgumentException("Argument zipcode does not exist in list: " + zipcode);
        return addresses.get(0);
    }

    public static void load() {
        if (map != null)
            return;
        String sourcePath = System.getenv(SOURCE_DIR_ENV_NAME) + "/" + SOURCE_FILE_BASENAME;
        map = I5FJsonData.loadMap(sourcePath, new TypeReference<Map<String,List<String>>>(){});
        shuffled = new ArrayList<String>();
        map.forEach((k,v) -> { shuffled.add(k); });
        Collections.shuffle(shuffled);
    }
}
