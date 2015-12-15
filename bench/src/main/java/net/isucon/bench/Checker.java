package net.isucon.bench;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.function.Supplier;
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

    public enum States {
        MODERATE,
        CRITICAL,
        FATAL,
    }

    private States state;

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

        this.state = States.CRITICAL;
    }

    public Checker checkerAs(String format) {
        switch (format) {
        case "html": return new HtmlChecker(this.result, this.type, this.config, this.responseTime, this.response);
        case "json": return new JsonChecker(this.result, this.type, this.config, this.responseTime, this.response);
        }
        throw new IllegalArgumentException("unknown format for checker: " + format);
    }

    public void fatal() {
        this.state = States.FATAL;
    }

    public void critical() {
        this.state = States.CRITICAL;
    }

    public void moderate() {
        this.state = States.MODERATE;
    }

    protected boolean wrap(boolean success) {
        if (state == States.FATAL && !success) {
            throw new Driver.ScenarioAbortException();
        }
        if (state == States.CRITICAL && !success) {
            throw new Driver.CheckerCriticalFailureException();
        }
        return success;
    }

    public class Depender {
        private boolean success;
        private Checker checker;

        public Depender(Checker checker, boolean[] tests) {
            this.checker = checker;
            this.success = true;
            for (boolean b : tests) {
                if (!b) {
                    this.success = false;
                    break;
                }
            }
        }

        public Depender(Checker checker, Supplier<Boolean>[] tests) {
            this.checker = checker;
            this.success = true;
            for (Supplier<Boolean> s : tests) {
                if (! s.get().booleanValue()) {
                    this.success = false;
                    break;
                }
            }
        }

        public boolean then(Runnable nested) {
            if (success) {
                nested.run();
            }
            return success;
        }
    }

    public Depender depends(boolean... successes) {
        return new Depender(this, successes);
    }

    public Depender depends(Supplier<Boolean>... funcs) {
        return new Depender(this, funcs);
    }

    public boolean hasViolations() {
        return result.violations.size() > 0;
    }

    public void addViolation(String description) {
        result.addViolation(type, description);
    }

    public void fatal(String message) {
        addViolation(message);
        result.fail();
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

    public boolean isStatus(int status) {
        if (response.getStatus() != status) {
            String msg = "パス '%s' へのレスポンスコード %d が期待されていましたが %d でした";
            addViolation(String.format(msg, response.getRequest().getPath(), status, response.getStatus()));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean isRedirect(String path) {
        int status = response.getStatus();
        if (status != 302 && status != 303 && status != 307) {
            addViolation(String.format("レスポンスコードが一時リダイレクトのもの(302, 303, 307)ではなく %d でした", status));
            return wrap(false);
        }

        String value = response.getHeaders().get("Location");
        if (value == null) {
            addViolation(String.format("Location ヘッダがありません"));
            return wrap(false);
        } else if (value.equals(config.uri(path)) || value.equals(config.uriDefaultPort(path))) {
            return wrap(true);
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
                return wrap(true);
        }
        addViolation(String.format("リダイレクト先が %s でなければなりませんが %s でした", path, value));
        return wrap(false);
    }

    public boolean isContentLength(long bytes) {
        String value = response.getHeaders().get("Content-Length");
        if (value == null) {
            addViolation(String.format("リクエストパス %s に対して Content-Length がありませんでした", response.getRequest().getPath()));
            return wrap(false);
        } else if (Long.parseLong(value) == bytes) {
            return wrap(true);
        } else {
            addViolation(String.format("パス %s に対するレスポンスのサイズが正しくありません: %s bytes", response.getRequest().getPath(), value));
            return wrap(false);
        }
    }

    public boolean isContentType(String type) {
        if (! contentType().equals(type)) {
            addViolation(String.format("Content-Type ヘッダが %s になっていません: %s", type, contentType()));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean isContentBodyChecksum(String checksum) {
        if (! contentBodyChecksum.toUpperCase().equals(checksum.toUpperCase())) {
            addViolation(String.format("パス %s のcontent bodyの内容が一致しません", response.getRequest().getPath()));
            return wrap(false);
        }
        return wrap(true);
    }

    public boolean isValidHtml() {
        addViolation("正しいHTMLコンテンツではありません");
        return wrap(false);
    }

    public boolean isValidJson() {
        addViolation("正しいJSONコンテンツではありません");
        return wrap(false);
    }

    public boolean contentMatch(String pattern) {
        String contentBody = contentBody();
        if (contentBody.indexOf(pattern) < 0 && ! Pattern.compile(pattern).matcher(contentBody()).matches()) {
            addViolation(String.format("Content bodyに文字列パターン '%s' がみつかりません", pattern));
            return wrap(false);
        }
        return wrap(true);
    }

    // checker methods for subclasses

    private UnsupportedOperationException notSupportedException(){
        return new UnsupportedOperationException(String.format("this method isn't supported for content type: %s", contentType()));
    }

    public Document document() { throw notSupportedException(); }
    public Matcher lastMatch() { throw notSupportedException(); }
    public List find(String selector) { throw notSupportedException(); }
    public boolean hasStyleSheet(String path) { throw notSupportedException(); }
    public boolean hasJavaScript(String path) { throw notSupportedException(); }
    public boolean exist(String selector) { throw notSupportedException(); }
    public boolean exist(String selector, int num) { throw notSupportedException(); }
    public boolean missing(String selector) { throw notSupportedException(); }
    public boolean content(String selector, String text) { throw notSupportedException(); }
    public boolean contentMissing(String selector, String text) { throw notSupportedException(); }
    public boolean contentLongText(String selector, String text) { throw notSupportedException(); }
    public boolean contentMatch(String selector, String regexp) { throw notSupportedException(); }
    public boolean contentCheck(String selector, String message, Predicate<Element> callback) { throw notSupportedException(); }
    public boolean attribute(String selector, String attributeName, String text) { throw notSupportedException(); }
}
