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
        int index = random.nextInt(4) * 3;

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
                    check.exist("form.form-signin[action=/login]");
                    check.exist("input[name=email]");
                    check.exist("input[name=password]");
                    check.exist("button[type=submit]");
                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/signin.css");
                });
            getAndCheck(session, "/css/bootstrap.min.css", "BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxxx"); //TODO: fix this
                });
            getAndCheck(session, "/css/signin.css", "SIGNIN CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxx"); //TODO: fix this
                });
        }
        {
            getAndCheck(session, "/signup", "GET SIGNUP PAGE", (check) -> {
                    check.isStatus(200);
                    check.exist("form.form-signin[action=/signup]");
                    check.exist("input[name=email]");
                    check.exist("input[name=password]");
                    check.exist("select[name=grade]");
                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/signin.css");
                });
            getAndCheck(session, "/css/bootstrap.min.css", "BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxxx"); //TODO: fix this
                });
            getAndCheck(session, "/css/signin.css", "SIGNIN CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxx"); //TODO: fix this
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
                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/signin.css");
                });
            getAndCheck(session, "/css/bootstrap.min.css", "BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxxx"); //TODO: fix this
                });
            getAndCheck(session, "/css/signin.css", "SIGNIN CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxx"); //TODO: fix this
                });

            Map form = new HashMap();
            I5FParameter param = (I5FParameter) session.param();
            form.put("email", param.email);
            form.put("password", param.password);

            postAndCheck(session3, "/login", form, "POST LOGIN AFTER SIGNUP", (check) -> {
                    check.isRedirect("/");
                });
        }

        {
            getAndCheck(session, "/", "INDEX AFTER LOGIN", (check) -> {
                    check.isStatus(200);
                    check.hasStyleSheet("/css/bootstrap.min.css");
                    check.hasStyleSheet("/css/jumbotron-narrow.css");
                    check.hasJavaScript("/js/jquery-1.11.3.js");
                    check.hasJavaScript("/js/bootstrap.js");
                    check.hasJavaScript("/user.js");
                    check.hasJavaScript("/js/airisu.js");
                    //TODO: add content check on HTML
                });
            getAndCheck(session, "/css/bootstrap.min.css", "BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxxx"); //TODO: fix this
                });
            getAndCheck(session, "/css/jumbotron-narrow.css", "ADDITIONAL STYLE CSS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxxx"); //TODO: fix this
                });
            getAndCheck(session, "/js/jquery-1.11.3.js", "JQUERY JS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxxx"); //TODO: fix this
                });
            getAndCheck(session, "/js/bootstrap.js", "BOOTSTRAP JS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxxx"); //TODO: fix this
                });
            getAndCheck(session, "/js/airisu.js", "AIR-ISU JS", (check) -> {
                    check.isStatus(200);
                    check.isContentBodyChecksum("xxxxxx"); //TODO: fix this
                });
            String interval = null;
            I5FParameter param = (I5FParameter) session.param();
            switch (param.grade) {
            case "micro":    interval = "30000"; break;
            case "small":    interval = "30000"; break;
            case "standard": interval = "20000"; break;
            case "premium":  interval = "10000"; break;
            }
            getAndCheck(session, "/user.js", "JQUERY JS", (check) -> {
                    check.isStatus(200);
                    // micro 30000, small 30000, standard 20000, premium 10000
                    check.contentMatch(String.format("var AIR_ISU_REFRESH_INTERVAL = %s;", interval));
                });
        }

        {
            // TODO: get first /data (blank)
        }

        {
            // TODO: get /modify
            // TODO: post /modify
            // TODO: get /modify and check response
        }

        {
            // TODO: get /data and check content
        }
    }
}
