package net.isucon.isucon5q.bench.checker;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.api.Response;

// import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;

import net.isucon.isucon5q.bench.Checker;

import net.isucon.isucon5q.bench.Result;
import net.isucon.isucon5q.bench.Config;

public class JsonChecker extends Checker {
    private Object parsed = null;

    public JsonChecker(Result result, String type, Config config, long responseTime, Response response) {
        super(result, type, config, responseTime, response);
    }

    public Object parsed() {
        if (parsed != null) {
            return parsed;
        }

        try {
            this.parsed = Configuration.defaultConfiguration().jsonProvider().parse(contentBody());
        } catch (InvalidJsonException e) {
            // ignore, but parsed is left as null
        }
        return parsed;
    }

    public void isValidJson() {
        if (parsed() == null) {
            addViolation("Content bodyが有効なJSONではありません");
        }
    }

    /*
    public Map<String,Object> jsonMapContent() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(contentBody, Map.class);
    }

    public List<Object> jsonListContent() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(contentBody, List.class);
    }
    */

    public void exist(String selector) {
    }

    public void exist(String selector, int num) {
    }

    public void missing(String selector) {
    }

    public void content(String selector, String text) {
    }

    public void contentMissing(String selector, String text) {
    }

    public void contentMatch(String selector, String regexp) {
    }
}
