package net.isucon.bench;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public abstract class Parameter {
    public abstract String[] keys();
    public abstract String[] objects();

    public abstract void put(String name, String value);
    public abstract void putObject(String name, Map value);

    public static List<Parameter> generate(String className, String json) throws ClassNotFoundException {
        if (json.equals("dummy")) {
            return dummyParameters(className);
        }
        Class paramClass = Class.forName(className);

        List<Parameter> list = new ArrayList<Parameter>();

        JsonNode root = null;
        try {
            root = (new ObjectMapper()).readTree(json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse parameter json");
        }

        if (root.isArray()) {
            for (JsonNode n : root) {
                list.add(getParameterFromJsonNode(paramClass, n));
            }
        } else {
            // just single parameter object
            list.add(getParameterFromJsonNode(paramClass, root));
        }

        return list;
    }

    private static Parameter getParameterFromJsonNode(Class paramClass, JsonNode n) {
        Parameter p = getInstance(paramClass);
        for (String k : p.keys()) {
            JsonNode j = n.get(k);
            if (j != null)
                p.put(k, j.asText());
        }
        for (String k : p.objects()) {
            JsonNode j = n.get(k);
            if (j != null)
                p.putObject(k, (Map) j.get(k));
        }
        return p;
    }

    private static Parameter getInstance(Class klass) {
        Parameter p = null;
        try {
            p = (Parameter) klass.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Cannot create instance of %s", klass.getName()));
        }
        return p;
    }

    public static List<Parameter> dummyParameters(String className) throws ClassNotFoundException {
        throw new RuntimeException("not implemented");
    }
}
