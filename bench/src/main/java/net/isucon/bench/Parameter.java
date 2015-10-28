package net.isucon.bench;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class Parameter {
    public abstract String[] keys();
    public abstract String[] objects();

    public abstract void put(String name, String value);
    public abstract void putObject(String name, JsonNode node);

    private String source;

    public void setSource(String json) {
        this.source = json;
    }

    public String getSource() {
        return source;
    }

    public static List<Parameter> generate(String className, String json) throws ClassNotFoundException {
        if (json.equals("dummy")) {
            return dummyParameters(className);
        }
        Class paramClass = Class.forName(className);

        List<Parameter> list = new ArrayList<Parameter>();

        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = null;
        try {
            root = mapper.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse parameter json");
        }

        if (root.isArray()) {
            for (JsonNode n : root) {
                String src = "Failed to stringify";
                try { src = mapper.writeValueAsString(n); }
                catch (JsonProcessingException e) { }
                list.add(getParameterFromJsonNode(paramClass, n, src));
            }
        } else {
            String src = "Failed to stringify";
            try { src = mapper.writeValueAsString(root); }
            catch (JsonProcessingException e) { }
            // just single parameter object
            list.add(getParameterFromJsonNode(paramClass, root, src));
        }

        return list;
    }

    private static Parameter getParameterFromJsonNode(Class paramClass, JsonNode n, String json) {
        Parameter p = getInstance(paramClass, json);
        for (String k : p.keys()) {
            JsonNode j = n.get(k);
            if (j != null)
                p.put(k, j.asText());
        }
        for (String k : p.objects()) {
            JsonNode j = n.get(k);
            if (j != null && j.isObject()) {
                p.putObject(k, j);
            }
        }
        return p;
    }

    private static Parameter getInstance(Class klass, String json) {
        Parameter p = null;
        try {
            p = (Parameter) klass.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Cannot create instance of %s", klass.getName()));
        }
        p.setSource(json);
        return p;
    }

    public static List<Parameter> dummyParameters(String className) throws ClassNotFoundException {
        throw new RuntimeException("not implemented");
    }
}
