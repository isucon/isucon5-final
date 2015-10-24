package net.isucon.isucon5q.bench.scenario;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.isucon.isucon5q.bench.Scenario;
import net.isucon.isucon5q.bench.Step;
import net.isucon.isucon5q.bench.Result;
import net.isucon.isucon5q.bench.Parameter;
import net.isucon.isucon5q.bench.Session;

public class Isucon5FinalBootstrap extends Isucon5FinalBase {
    public Isucon5FinalBootstrap(Long timeout) {
        super(timeout);
    }

    @Override
    public Result finishHook(Result result) {
        if (result.responses.exception > 0 || result.violations.size() > 0) {
            result.fail();
        }
        return result;
    }

    @Override
    public boolean verbose() {
        return true;
    }

    private class CheckingStatus {
        //TODO: fixit
        public int friends;

        public Long existingEntryId;
        public String commentText;

        public Long postedEntryId;
        public String postedTitle;
        public String postedContent;

        public String newBirthday;
    }

    private static String[] BOOTSTRAP_TEST_DATA = new String[]{
        "tony@moris.io", "tonyny31", "micro",
        "tony@moris.io", "tonyny32", "small",
        "tony@moris.io", "tonyny33", "standard",
        "tony@moris.io", "tonyny34", "premium",
    };

    private static Session testData() {
        Random rand = new Random();
        int index = rand.nextInt(4) * 3;

        I5FParameter param = new I5FParameter();
        param.email = BOOTSTRAP_TEST_DATA[index];
        param.password = BOOTSTRAP_TEST_DATA[index + 1];
        param.grade = BOOTSTRAP_TEST_DATA[index + 2];

        param.putDummySubscriptions();

        return new Session(param);
    }

    @Override
    public void scenario(List<Session> originalSessions) {
        Session session = testData();

        {
            getAndCheck(session, "/", "GET INDEX BEFORE SIGNUP", (check) -> { check.isRedirect("/login"); });
            getAndCheck(session, "/login", "GET LOGIN BEFORE SIGNUP", (check) -> {
                    check.isStatus(200);
                    check.isContentType("text/html");
                    check.exist("form.form-signin[action=/login]");
                    check.exist("input[name=email]");
                    check.exist("input[name=password]");
                    check.exist("button[type=submit]");
                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/signin.css");
                });
            getAndCheck(session, "/css/bootstrap.min.css", "BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentLength(122540);
                    check.isContentBodyChecksum("08df9a96752852f2cbd310c30facd934e348c2c5");
                });
            getAndCheck(session, "/css/signin.css", "SIGNIN CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("702783cc5eff3d8d3532e339ddd15c57f7a08776");
                });
        }
        {
            getAndCheck(session, "/signup", "GET SIGNUP PAGE", (check) -> {
                    check.isStatus(200);
                    check.isContentType("text/html");
                    check.exist("form.form-signin[action=/signup]");
                    check.exist("input[name=email]");
                    check.exist("input[name=password]");
                    check.exist("select[name=grade]");
                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/signin.css");
                });
            getAndCheck(session, "/css/bootstrap.min.css", "BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("08df9a96752852f2cbd310c30facd934e348c2c5");
                });
            getAndCheck(session, "/css/signin.css", "SIGNIN CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("702783cc5eff3d8d3532e339ddd15c57f7a08776");
                });
            
            Map form = new HashMap();
            I5FParameter param = (I5FParameter) session.param();
            form.put("email", param.email);
            form.put("password", param.password);
            form.put("grade", param.grade);
            postAndCheck(session, "/signup", form, "SIGNUP FOR BOOTSTRAP CHECK", (check) -> {
                    check.isRedirect("/login");
                });
        }

        {
            getAndCheck(session, "/login", "GET LOGIN PAGE AFTER SIGNUP", (check) -> {
                    check.isStatus(200);
                    check.isContentType("text/html");
                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/signin.css");
                });
            getAndCheck(session, "/css/bootstrap.min.css", "BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("08df9a96752852f2cbd310c30facd934e348c2c5");
                });
            getAndCheck(session, "/css/signin.css", "SIGNIN CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("702783cc5eff3d8d3532e339ddd15c57f7a08776");
                });

            Map form = new HashMap();
            I5FParameter param = (I5FParameter) session.param();
            form.put("email", param.email);
            form.put("password", param.password);

            postAndCheck(session, "/login", form, "POST LOGIN AFTER SIGNUP", (check) -> {
                    check.isRedirect("/");
                });
        }

        {
            I5FParameter param = (I5FParameter) session.param();
            getAndCheck(session, "/", "INDEX AFTER LOGIN", (check) -> {
                    check.isStatus(200);
                    check.isContentType("text/html");
                    check.exist(".container .header.clearfix nav ul.nav li a[href=/modify]");
                    check.content(".container .header h3", String.format("AirISU: %s", param.email));

                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/jumbotron-narrow.css");
                    check.hasJavaScript("/js/jquery-1.11.3.js");
                    check.hasJavaScript("/js/bootstrap.js");
                    check.hasJavaScript("/user.js");
                    check.hasJavaScript("/js/airisu.js");
                });
            getAndCheck(session, "/css/bootstrap.min.css", "BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("08df9a96752852f2cbd310c30facd934e348c2c5");
                });
            getAndCheck(session, "/css/jumbotron-narrow.css", "ADDITIONAL STYLE CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("d55e584b9bb64d574c09ab02e361a4e49a1e6b5f");
                });
            getAndCheck(session, "/js/jquery-1.11.3.js", "JQUERY JS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("96c73f3774471cc8378c77a64ecf09b7f625d8b7");
                });
            getAndCheck(session, "/js/bootstrap.js", "BOOTSTRAP JS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("bbf55e20f1ebb6368522799f29db39830a08ef93");
                });
            getAndCheck(session, "/js/airisu.js", "AIR-ISU JS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("11f090f858fcc93fc83504281ceea03eb03bff46");
                });
            String intervalVal = null;
            switch (param.grade) {
            case "micro":    intervalVal = "30000"; break;
            case "small":    intervalVal = "30000"; break;
            case "standard": intervalVal = "20000"; break;
            case "premium":  intervalVal = "10000"; break;
            }
            final String interval = intervalVal;
            getAndCheck(session, "/user.js", "JQUERY JS", (check) -> {
                    check.isStatus(200);
                    check.contentMatch(String.format("var AIR_ISU_REFRESH_INTERVAL = %s;", interval));
                });
        }

        {
            getAndCheck(session, "/data", "DATA FOR BLANK SUBSCRIPTIONS", (check) -> {
                    check.isStatus(200);
                    check.isContentType("application/json");
                    check.isValidJson();
                    check.missing("$..*");
                });
        }

        {
            I5FParameter param = (I5FParameter) session.param();
            final String email = param.email;
            final String grade = param.grade;
            getAndCheck(session, "/modify", "GET MODIFY PAGE", (check) -> {
                    check.isStatus(200);
                    check.attribute(".container", "data-grade", grade);
                    check.content(".container .header h3", String.format("AirISU 設定変更: %s", email));
                    switch (grade) {
                    case "premium":
                        // highest grade
                    case "standard":
                        // next
                    case "small":
                        // yay
                    default:
                        check.exist(".api-form[data-service=ken]");
                        check.exist(".api-form[data-service=ken2]");
                        check.exist(".api-form[data-service=surname]");
                        check.exist(".api-form[data-service=givenname]");
                    }
                });
            {
                HashMap<String,String> form = new HashMap<String,String>();
                form.put("service", "ken");
                form.put("keys", String.join(" ", param.subscriptions.get("ken").keys));
                postAndCheck(session, "/modify", form, "MODIFY KEN", (check) -> {
                        check.isRedirect("/modify");
                    });
            }
            {
                HashMap<String,String> form = new HashMap<String,String>();
                form.put("service", "ken2");
                form.put("param_name", "zipcode");
                form.put("param_value", param.subscriptions.get("ken2").params.get("zipcode"));
                postAndCheck(session, "/modify", form, "MODIFY KEN2", (check) -> {
                        check.isRedirect("/modify");
                    });
            }
            // TODO: modify surname, givenname
        }

        {
            I5FParameter param = (I5FParameter) session.param();
            getAndCheck(session, "/", "GET INDEX AFTER MODIFY", (check) -> {
                    check.isStatus(200);
                    check.isContentType("text/html");
                    check.exist(".container .header.clearfix nav ul.nav li a[href=/modify]");
                    check.content(".container .header h3", String.format("AirISU: %s", param.email));

                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/jumbotron-narrow.css");
                    check.hasJavaScript("/js/jquery-1.11.3.js");
                    check.hasJavaScript("/js/bootstrap.js");
                    check.hasJavaScript("/user.js");
                    check.hasJavaScript("/js/airisu.js");
                });
            String kenValue = I5FZipcodes.address(param.subscriptions.get("ken").keys.get(0));
            String ken2Value = I5FZipcodes.address(param.subscriptions.get("ken2").params.get("zipcode"));
            getAndCheck(session, "/data", "DATA AFTER MODIFY SUBSCRIPTIONS", (check) -> {
                    check.isStatus(200);
                    check.isContentType("application/json");
                    check.isValidJson();
                    check.exist("$.[?(@.service=='ken')]", 1);
                    check.contentMatch("$.[?(@.service=='ken')].data.addresses[0]", kenValue);
                    check.exist("$.[?(@.service=='ken2')]", 1);
                    check.contentMatch("$.[?(@.service=='ken2')].data.addresses[0]", ken2Value);
                });
        }
    }
}
