package net.isucon.bench.checker;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.api.Response;

// import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.PathNotFoundException;

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

    public boolean isValidJson() {
        if (parsed() == null) {
            addViolation("Content bodyが有効なJSONではありません");
            return wrap(false);
        }
        return wrap(true);
    }

    public List find(String selector) {
        try{
            return JsonPath.read(parsed(), selector);
        } catch (PathNotFoundException e) {
            return Collections.emptyList();
        }
    }

    public boolean exist(String selector) {
        try {
            List list = (List) JsonPath.read(parsed(), selector);
            if (list.size() > 0) {
                addViolation(String.format("要素 %s が存在するはずですが、存在しません", selector));
                return wrap(true);
            }
            return wrap(true);
        } catch (PathNotFoundException e) {
            addViolation(String.format("要素 %s が存在するはずですが存在しません", selector));
            return wrap(false);
        }
    }

    public boolean exist(String selector, int num) {
        try {
            List list = (List) JsonPath.read(parsed(), selector);
            if (list.size() != num) {
                addViolation(String.format("要素 %s が %d オブジェクト存在するはずですが、異なっています", selector, num));
                return wrap(false);
            }
            return wrap(true);
        } catch (PathNotFoundException e) {
            addViolation(String.format("要素 %s が %d オブジェクト存在するはずですが異なっています", selector, num));
            return wrap(false);
        }
    }

    public boolean missing(String selector) {
        try {
            List list = (List) JsonPath.read(parsed(), selector);
            if (list.size() != 0) {
                addViolation(String.format("要素 %s が存在しないはずですが、存在します", selector));
                return wrap(false);
            }
            return wrap(true);
        } catch (PathNotFoundException e) {
            return wrap(true);
        }
    }

    public boolean content(String selector, String text) {
        try {
            List<String> list = JsonPath.read(parsed(), selector);
            if (list.size() == 1 && list.get(0).equals(text)) {
                return wrap(true);
            } else {
                addViolation(String.format("要素 %s の内容が %s ではありません", selector, text));
                return wrap(false);
            }
        } catch (PathNotFoundException e) {
            addViolation(String.format("要素 %s の内容が %s のはずですが要素が存在しません", selector, text));
            return wrap(false);
        }
    }

    // public void contentMissing(String selector, String text) {
    // }

    public boolean contentMatch(String selector, String value) {
        try {
            List<String> list = JsonPath.read(parsed(), selector);
            if (list.size() == 1) {
                if (! list.get(0).equals(value)) {
                    addViolation(String.format("リスト %s の要素が '%s' と一致しません", selector, value));
                    return wrap(false);
                }
                return wrap(true);
            } else {
                boolean match = false;
                for (String s : list) {
                    if (s.equals(value)) {
                        match = true;
                        break;
                    }
                }
                if (! match) {
                    addViolation(String.format("リスト %s の要素の中に '%s' と一致するものがありません", selector, value));
                    return wrap(false);
                }
                return wrap(true);
            }
        } catch (PathNotFoundException e) {
            addViolation(String.format("リスト %s の要素の中に '%s' がありません", selector, value));
            return wrap(false);
        }
    }
}
