package net.isucon.bench.checker;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.List;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.api.Response;

// import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;

import net.isucon.bench.Checker;

import net.isucon.bench.Result;
import net.isucon.bench.Config;

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

    public List find(String selector) {
        return JsonPath.read(parsed(), selector);
    }

    public void exist(String selector) {
        if (((List) JsonPath.read(parsed(), selector)).size() > 0) {
            addViolation(String.format("要素 %s が存在するはずですが、存在しません", selector));
        }
    }

    public void exist(String selector, int num) {
        if (((List) JsonPath.read(parsed(), selector)).size() != num) {
            addViolation(String.format("要素 %s が %d オブジェクト存在するはずですが、異なっています", selector, num));
            System.err.println(contentBody());
        }
    }

    public void missing(String selector) {
        if (((List) JsonPath.read(parsed(), selector)).size() != 0) {
            addViolation(String.format("要素 %s が存在しないはずですが、存在します", selector));
        }
    }

    public void content(String selector, String text) {
        List<String> list = JsonPath.read(parsed(), selector);
        if (list.size() == 1 && list.get(0).equals(text)) {
            // ok
        } else {
            addViolation(String.format("要素 %s の内容が %s ではありません", selector, text));
        }
    }

    // public void contentMissing(String selector, String text) {
    // }

    public void contentMatch(String selector, String value) {
        List<String> list = JsonPath.read(parsed(), selector);
        if (list.size() == 1) {
            if (! list.get(0).equals(value)) {
                addViolation(String.format("リスト %s の要素が '%s' と一致しません", selector, value));
            }
        } else {
            boolean match = false;
            for (String s : list) {
                if (s.equals(value)) {
                    match = true;
                    break;
                }
            }
            if (! match)
                addViolation(String.format("リスト %s の要素の中に '%s' と一致するものがありません", selector, value));
        }
    }
}
