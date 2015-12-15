package net.isucon.bench.checker;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.api.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.isucon.bench.Checker;

import net.isucon.bench.Result;
import net.isucon.bench.Config;

public class HtmlChecker extends Checker {
    private Document parsedDocument;
    private Matcher lastMatch;

    public HtmlChecker(Result result, String type, Config config, long responseTime, Response response) {
        super(result, type, config, responseTime, response);
        this.parsedDocument = null;
    }

    public boolean isValidHtml() {
        // parsing html is too hard ... so this class estimates that content is valid
        return wrap(true);
    }

    public Document document() {
        if (contentBody() == null) {
            throw new IllegalStateException();
        }
        if (parsedDocument != null) {
            return parsedDocument;
        }
        parsedDocument = Jsoup.parse(contentBody());
        return parsedDocument;
    }
    
    public Matcher lastMatch() {
        return lastMatch;
    }

    // find -> List<String>: text? something else?

    public boolean hasStyleSheet(String path) {
        Elements es = document().head().getElementsByTag("link");
        if (es.stream().noneMatch(e -> e.attr("rel").equals("stylesheet") && e.attr("href").equals(path))) {
            addViolation(String.format("スタイルシートのパス %s への参照がありません", path));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean hasJavaScript(String path) {
        Elements es = document().body().getElementsByTag("script");
        if (es.stream().noneMatch(e -> e.attr("src").equals(path))) {
            addViolation(String.format("JavaScriptファイルのパス %s への参照がありません", path));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean exist(String selector) {
        if (document().select(selector).size() == 0) {
            addViolation(String.format("指定のDOM要素 '%s' が見付かりません", selector));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean exist(String selector, int num) {
        if (document().select(selector).size() != num) {
            addViolation(String.format("指定のDOM要素 '%s' が %d 回表示されるはずですが、正しくありません", selector, num));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean missing(String selector) {
        if (document().select(selector).size() > 0) {
            addViolation(String.format("DOM要素 '%s' は表示されないはずですが、表示されています", selector));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean content(String selector, String text) {
        Elements es = document().select(selector);
        if (es.stream().noneMatch(e -> e.hasText() && e.text().trim().equals(text))) {
            if (es.size() == 1) {
                addViolation(String.format("DOM要素 '%s' に文字列 '%s' がセットされているはずですが '%s' となっています", selector, text, es.first().text()));
                return wrap(false);
            } else {
                addViolation(String.format("DOM要素 '%s' で文字列 '%s' をもつものが見付かりません", selector, text));
                return wrap(false);
            }
        }
        return wrap(true);
    }

    public boolean contentMissing(String selector, String text) {
        Elements es = document().select(selector);
        if (es.stream().anyMatch(e -> e.hasText() && e.text().trim().equals(text))) {
            addViolation(String.format("DOM要素 '%s' に文字列 '%s' をもつものは表示されないはずですが、表示されています", selector, text));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean contentLongText(String selector, String text) {
        String shrinkText = Arrays.stream(text.split("\n")).map(v -> v.trim()).collect(Collectors.joining());
        Elements es = document().select(selector);
        for (Element e : es) {
            String fullText = Arrays.stream(e.html().trim().split("<(br|BR|Br|bR) */?>"))
                .map(v -> Arrays.stream(v.trim().split("\n")).collect(Collectors.joining()))
                .collect(Collectors.joining(""));
            if (fullText.equals(shrinkText))
                return wrap(true);
        }
        addViolation(String.format("入力されたはずのテキストがDOM要素 '%s' に表示されていません", selector));
        return wrap(false);
    }

    public boolean contentMatch(String selector, String regexp) {
        Elements es = document().select(selector);
        if (es.size() == 1) {
            lastMatch = (Pattern.compile(regexp, Pattern.MULTILINE)).matcher(es.first().text());
            if (! lastMatch.matches()) {
                addViolation(String.format("DOM要素 '%s' のテキストが正規表現 '%s' にマッチしません", selector, regexp));
                return wrap(false);
            }
        } else {
            Pattern p = Pattern.compile(regexp, Pattern.MULTILINE);
            boolean match = false;
            for (Element e : es) {
                if (p.matcher(e.text()).matches()) {
                    match = true;
                    break;
                }
            }
            if (! match) {
                addViolation(String.format("DOM要素 '%s' の中に、テキストが正規表現 '%s' にマッチするものが見付かりません", selector, regexp));
                return wrap(false);
            }
        }
        return wrap(true);
    }

    public boolean contentCheck(String selector, String message, Predicate<Element> callback) {
        boolean ok = false;
        for (Element e : document().select(selector)) {
            if (callback.test(e)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            addViolation(message);
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean attribute(String selector, String attributeName, String text) {
        Elements es = document().select(selector);
        if (es.stream().noneMatch(e -> e.attr(attributeName).equals(text))) {
            addViolation(String.format("DOM要素 '%s' のattribute %s の内容が '%s' になっていません", selector, attributeName, text));
            return wrap(false);
        }
        return wrap(true);
    }
}
