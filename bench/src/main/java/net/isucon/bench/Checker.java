package net.isucon.bench;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.eclipse.jetty.client.api.Response;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import net.isucon.bench.checker.HtmlChecker;
import net.isucon.bench.checker.JsonChecker;

public class Checker {
    private Result result;
    private String type;
    private Config config;
    private long responseTime;
    private Response response;

    private String contentType;

    private String contentBodyChecksum;
    private String contentBody;

    public static Checker create(Result result, String type, Config config, long responseTime, Response response) {
        String contentType = response.getHeaders().get("Content-Type");
        if (contentType == null)
            return new Checker(result, type, config, responseTime, response);

        if (contentType.indexOf(";") > -1) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
        }
        switch (contentType) {
        case "text/html": return new HtmlChecker(result, type, config, responseTime, response);
        case "application/json": return new JsonChecker(result, type, config, responseTime, response);
        }
        return new Checker(result, type, config, responseTime, response);
    }

    public Checker(Result result, String type, Config config, long responseTime, Response response) {
        this.result = result;
        this.type = type;
        this.config = config;
        this.responseTime = responseTime;
        this.response = response;

        this.contentType = response.getHeaders().get("Content-Type");
        if (contentType != null && contentType.indexOf(";") > -1) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
        }

        this.contentBodyChecksum = null; // SHA-1 checksum
        this.contentBody = null;
    }

    public Checker checkerAs(String format) {
        switch (format) {
        case "html": return new HtmlChecker(this.result, this.type, this.config, this.responseTime, this.response);
        case "json": return new JsonChecker(this.result, this.type, this.config, this.responseTime, this.response);
        }
        throw new IllegalArgumentException("unknown format for checker: " + format);
    }

    public boolean hasViolations() {
        return result.violations.size() > 0;
    }

    public void addViolation(String description) {
        result.addViolation(type, description);
    }

    public void fatal(String message) {
        addViolation(message);
        throw new Driver.ScenarioAbortException();
    }

    public void setContentBodyChecksum(String sha1sum) {
        this.contentBodyChecksum = sha1sum;
    }

    public void setContentBody(String body) {
        this.contentBody = body;
    }

    public Response response() {
        return response;
    }

    public String header(String header) {
        return response.getHeaders().get(header);
    }

    public String contentBody() {
        return contentBody;
    }

    public String contentType() {
        if (contentType == null) {
            return "unspecified";
        }
        return contentType;
    }

    public void respondUntil(long millis) {
        if (responseTime > millis) {
            addViolation(String.format("アプリケーションが %d ミリ秒以内に応答しませんでした", millis));
        }
    }

    public void isStatus(int status) {
        if (response.getStatus() != status) {
            String msg = "パス '%s' へのレスポンスコード %d が期待されていましたが %d でした";
            addViolation(String.format(msg, response.getRequest().getPath(), status, response.getStatus()));
        }
    }

    public void isRedirect(String path) {
        int status = response.getStatus();
        if (status != 302 && status != 303 && status != 307) {
            addViolation(String.format("レスポンスコードが一時リダイレクトのもの(302, 303, 307)ではなく %d でした", status));
            return;
        }

        String value = response.getHeaders().get("Location");
        if (value == null) {
            addViolation(String.format("Location ヘッダがありません"));
            return;
        } else if (value.equals(config.uri(path)) || value.equals(config.uriDefaultPort(path))) {
            return; // ok
        }

        URI uri = null;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            // invalid syntax
        }
        if (uri != null) {
            String h = uri.getHost();
            String p = uri.getPath();
            if (p.isEmpty())
                p = "/";
            if ((h == null || h.equals(config.host)) && p.equals(path))
                return; // ok
        }
        addViolation(String.format("リダイレクト先が %s でなければなりませんが %s でした", path, value));
    }

    public void isContentLength(long bytes) {
        String value = response.getHeaders().get("Content-Length");
        if (value == null) {
            addViolation(String.format("リクエストパス %s に対して Content-Length がありませんでした", response.getRequest().getPath()));
        } else if (Long.parseLong(value) == bytes) {
            // ok
        } else {
            addViolation(String.format("パス %s に対するレスポンスのサイズが正しくありません: %s bytes", response.getRequest().getPath(), value));
        }
    }

    public void isContentType(String type) {
        if (! contentType().equals(type)) {
            addViolation(String.format("Content-Type ヘッダが %s になっていません: %s", type, contentType()));
        }
    }

    public void isContentBodyChecksum(String checksum) {
        if (! contentBodyChecksum.toUpperCase().equals(checksum.toUpperCase())) {
            addViolation(String.format("パス %s のcontent bodyの内容が一致しません", response.getRequest().getPath()));
        }
    }

    public void isValidHtml() {
        addViolation("正しいHTMLコンテンツではありません");
    }

    public void isValidJson() {
        addViolation("正しいJSONコンテンツではありません");
    }

    public void contentMatch(String pattern) {
        String contentBody = contentBody();
        if (contentBody.indexOf(pattern) < 0 && ! Pattern.compile(pattern).matcher(contentBody()).matches()) {
            addViolation(String.format("Content bodyに文字列パターン '%s' がみつかりません", pattern));
        }
    }

    // checker methods for subclasses

    private UnsupportedOperationException notSupportedException(){
        return new UnsupportedOperationException(String.format("this method isn't supported for content type: %s", contentType()));
    }

    public Document document() { throw notSupportedException(); }
    public Matcher lastMatch() { throw notSupportedException(); }
    public List find(String selector) { throw notSupportedException(); }
    public void hasStyleSheet(String path) { throw notSupportedException(); }
    public void hasJavaScript(String path) { throw notSupportedException(); }
    public void exist(String selector) { throw notSupportedException(); }
    public void exist(String selector, int num) { throw notSupportedException(); }
    public void missing(String selector) { throw notSupportedException(); }
    public void content(String selector, String text) { throw notSupportedException(); }
    public void contentMissing(String selector, String text) { throw notSupportedException(); }
    public void contentLongText(String selector, String text) { throw notSupportedException(); }
    public void contentMatch(String selector, String regexp) { throw notSupportedException(); }
    public void contentCheck(String selector, String message, Predicate<Element> callback) { throw notSupportedException(); }
    public void attribute(String selector, String attributeName, String text) { throw notSupportedException(); }
}
