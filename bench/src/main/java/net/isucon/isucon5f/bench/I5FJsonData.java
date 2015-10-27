package net.isucon.isucon5f.bench;

import java.io.File;
import java.io.IOException;

import java.util.Map;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.core.JsonParseException;

public class I5FJsonData {
    protected static <T> T loadMap(String path, TypeReference<T> typeref) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(path), typeref);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json: " + path);
        }
    }

    protected static <T> T loadList(String path, TypeReference<T> typeref) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(path), typeref);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json: " + path);
        }
    }

    public static class NameElement {
        public String name;
        public String yomi;
    }

    public static class NameEntry {
        public String query;
        public List<NameElement> result;
    }
}
