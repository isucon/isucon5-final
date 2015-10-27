package net.isucon.isucon5f.bench;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;

public class I5FGivennames extends I5FJsonData {
    private static String SOURCE_DIR_ENV_NAME = "ISUCON_BENCH_DATADIR";
    private static String SOURCE_FILE_BASENAME = "givenname_queries_response.json";

    public static Map<String,I5FJsonData.NameEntry> map;
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

    public static String getQuery(String key) {
        load();
        return map.get(key).query;
    }

    public static List<I5FJsonData.NameElement> getResult(String key) {
        load();
        return map.get(key).result;
    }

    public static void load() {
        if (map != null)
            return;
        String sourcePath = System.getenv(SOURCE_DIR_ENV_NAME) + "/" + SOURCE_FILE_BASENAME;
        Map<String,I5FJsonData.NameEntry> input = I5FJsonData.loadMap(sourcePath, new TypeReference<Map<String,I5FJsonData.NameEntry>>(){});
        map = new HashMap<String,I5FJsonData.NameEntry>();
        shuffled = new ArrayList<String>();
        input.forEach((k,v) -> {
                map.put(k, v);
                shuffled.add(k);
            });
        Collections.shuffle(shuffled);
    }
}

