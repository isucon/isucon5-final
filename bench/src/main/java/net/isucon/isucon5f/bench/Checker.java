package net.isucon.isucon5f.bench;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;
import java.util.Date;

import java.net.URI;
import java.net.URISyntaxException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ChronoField;

import org.apache.http.client.utils.DateUtils;

import net.isucon.bench.Scenario;
import net.isucon.bench.Step;
import net.isucon.bench.Result;
import net.isucon.bench.Parameter;
import net.isucon.bench.Session;

public class Checker extends Base {
    private static final String PARAMETER_CLASS = "net.isucon.isucon5f.bench.I5FParameter";

    private static long DURATION_MILLIS = 60 * 1000;

    public Checker(Long timeout) {
        super(timeout);
    }

    @Override
    public String parameterClassName() {
        return PARAMETER_CLASS;
    }

    @Override
    public void scenario(List<Session> sessions) {
        System.err.println("Checker");
        Random random = new Random();

        LocalDateTime stopAt = LocalDateTime.now().plus(DURATION_MILLIS, ChronoUnit.MILLIS);

        while (true) {
            if (LocalDateTime.now().isAfter(stopAt))
                break;
            Session s = sessions.get(random.nextInt((int) sessions.size()));
            I5FParameter param = (I5FParameter) s.param();

            getAndCheck(s, "/login", "GET LOGIN", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.isContentType("text/html");
                        check.hasStyleSheet("/css/bootstrap.min.css");
                        check.hasStyleSheet("/css/signin.css");
                    }
                });
            getAndCheck(s, "/css/bootstrap.min.css", "GET BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.isContentBodyChecksum("08df9a96752852f2cbd310c30facd934e348c2c5");
                    }
                });
            getAndCheck(s, "/css/signin.css", "GET SIGNIN CSS", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.isContentBodyChecksum("702783cc5eff3d8d3532e339ddd15c57f7a08776");
                    }
                });
            postAndCheck(s, "/login", formLogin(s), "POST LOGIN", (check) -> {
                    check.isRedirect("/");
                });

            if (LocalDateTime.now().isAfter(stopAt))
                break;

            getAndCheck(s, "/", "GET INDEX", (check) -> {
                    check.isStatus(200);
                    check.isContentType("text/html");
                    if (! check.hasViolations()) {
                        check.exist(".container .header.clearfix nav ul.nav li a[href=/modify]");
                        check.content(".container .header h3", String.format("AirISU: %s", param.email));

                        check.hasStyleSheet("/css/bootstrap.min.css");
                        check.hasStyleSheet("/css/jumbotron-narrow.css");
                        check.hasJavaScript("/js/jquery-1.11.3.js");
                        check.hasJavaScript("/js/bootstrap.js");
                        check.hasJavaScript("/user.js");
                        check.hasJavaScript("/js/airisu.js");
                    }
                });
            getAndCheck(s, "/css/bootstrap.min.css", "GET BOOTSTRAP CSS", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.isContentBodyChecksum("08df9a96752852f2cbd310c30facd934e348c2c5");
                    }
                });
            getAndCheck(s, "/css/jumbotron-narrow.css", "GET JUMBOTRON CSS", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.isContentBodyChecksum("d55e584b9bb64d574c09ab02e361a4e49a1e6b5f");
                    }
                });
            getAndCheck(s, "/js/jquery-1.11.3.js", "GET JQUERY JS", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.isContentBodyChecksum("96c73f3774471cc8378c77a64ecf09b7f625d8b7");
                    }
                });
            getAndCheck(s, "/js/bootstrap.js", "GET BOOTSTRAP JS", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.isContentBodyChecksum("bbf55e20f1ebb6368522799f29db39830a08ef93");
                    }
                });
            getAndCheck(s, "/js/airisu.js", "GET AIR-ISU JS", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.isContentBodyChecksum("2a7e762957979ed3b2bf7ba3503a471b8ed76437");
                    }
                });
            String intervalVal = null;
            switch (param.grade) {
            case "micro":    intervalVal = "30000"; break;
            case "small":    intervalVal = "30000"; break;
            case "standard": intervalVal = "20000"; break;
            case "premium":  intervalVal = "10000"; break;
            }
            final String interval = intervalVal;
            getAndCheck(s, "/user.js", "GET USER JS", (check) -> {
                    check.isStatus(200);
                    if (! check.hasViolations()) {
                        check.contentMatch(String.format("var AIR_ISU_REFRESH_INTERVAL = %s;", interval));
                    }
                });

            if (LocalDateTime.now().isAfter(stopAt))
                break;

            String grade = param.grade;

            Consumer<net.isucon.bench.Checker> callback = (check) -> {
                check.isStatus(200);
                check.isContentType("application/json");
                check.isValidJson();
                if (check.hasViolations()) {
                    return;
                }

                String kenValue = I5FZipcodes.address(param.subscriptions.get("ken").keys.get(0));
                String ken2Value = I5FZipcodes.address(param.subscriptions.get("ken2").params.get("zipcode"));

                check.exist("$.[?(@.service=='ken')]", 1);
                if (! check.hasViolations())
                    check.contentMatch("$.[?(@.service=='ken')].data.addresses.*", kenValue);

                check.exist("$.[?(@.service=='ken2')]", 1);
                if (! check.hasViolations())
                    check.contentMatch("$.[?(@.service=='ken2')].data.addresses.*", ken2Value);

                check.exist("$.[?(@.service=='surname')].data.query", 1);
                if (! check.hasViolations()) {
                    String qKey = param.subscriptions.get("surname").params.get("q");
                    String query = I5FSurnames.getQuery(qKey);
                    String queryCheck = String.format("$.[?(@.service=='surname')].data.[?(@.query=='%s')]", query);
                    check.exist(queryCheck, 1);
                    List<I5FJsonData.NameElement> result = I5FSurnames.getResult(qKey);
                    for (I5FJsonData.NameElement r : result) {
                        check.contentMatch("$.[?(@.service=='surname')].data.result..name", r.name);
                    }
                }

                if (grade.equals("small") || grade.equals("standard") || grade.equals("premium")){
                    check.exist("$.[?(@.service=='givenname')].data.query", 1);
                    if (! check.hasViolations()) {
                        String qKey = param.subscriptions.get("givenname").params.get("q");
                        String query = I5FGivennames.getQuery(qKey);
                        String queryCheck = String.format("$.[?(@.service=='givenname')].data.[?(@.query=='%s')]", query);
                        check.exist(queryCheck, 1);
                        if (! check.hasViolations()) {
                            List<I5FJsonData.NameElement> result = I5FGivennames.getResult(qKey);
                            for (I5FJsonData.NameElement r : result) {
                                check.contentMatch("$.[?(@.service=='givenname')].data.result..name", r.name);
                            }
                        }
                    }
                } else {
                    check.missing("$.[?(@.service=='givenname')].data.query");
                }

                if (grade.equals("standard") || grade.equals("premium")){
                    check.exist("$.[?(@.service=='tenki')].data", 1);

                    if (! check.hasViolations()) {
                        String date = (String) check.find("$.[?(@.service=='tenki')].data.date").get(0);
                        long dataAt = DateUtils.parseDate(date).getTime();
                        String responseDate = check.header("Date");
                        long responseAt = new Date().getTime();
                        if (responseDate != null) {
                            responseAt = DateUtils.parseDate(responseDate).getTime();
                        }
                        if (responseAt - dataAt > I5FTenki.VALID_CACHE_MILLIS) {
                            check.fatal("Tenki API レスポンスの内容が古いままです");
                        }

                        String yoho = I5FTenki.getYoho(date);
                        check.content("$.[?(@.service=='tenki')].data.yoho", yoho);
                    }
                } else {
                    check.missing("$.[?(@.service=='tenki')].data");
                }

                if (grade.equals("premium")){
                    String responseDate = check.header("Date");
                    long responseAt = new Date().getTime();
                    if (responseDate != null) {
                        responseAt = DateUtils.parseDate(responseDate).getTime();
                    }

                    check.exist("$.[?(@.service=='perfectsec')].data", 1);
                    if (! check.hasViolations()) {
                        String req = param.subscriptions.get("perfectsec").params.get("req");
                        check.content("$.[?(@.service=='perfectsec')].data.req", req);

                        String token = param.subscriptions.get("perfectsec").token;
                        check.exist("$.[?(@.service=='perfectsec')].data.key", 1);
                        String key = (String) check.find("$.[?(@.service=='perfectsec')].data.key").get(0);

                        check.exist("$.[?(@.service=='perfectsec')].data.onetime_token", 1);
                        String onetime = (String) check.find("$.[?(@.service=='perfectsec')].data.onetime_token").get(0);
                        if (! I5FPerfectSecurity.isCorrectOneTime(onetime, token, req, key, responseAt)) {
                            check.fatal("perfectsec API onetime tokenの値が不正です");
                        }

                        check.exist("$.[?(@.service=='perfectsec_attacked')].data", 1);
                        check.exist("$.[?(@.service=='perfectsec_attacked')].data.updated_at", 1);
                        List found = check.find("$.[?(@.service=='perfectsec_attacked')].data.updated_at");
                        if (! check.hasViolations() && found != null) {
                            // seconds from epoch
                            long epoch = Long.getLong(String.valueOf(found.get(0)));
                            long epochMillis = epoch * 1000;
                            if (responseAt - epochMillis > I5FPerfectSecurity.VALID_CACHE_MILLIS) {
                                check.fatal("perfectsec_attacked API レスポンスの内容が古いままです");
                            }
                            String[] attacked = I5FPerfectSecurity.getAttacked(token, Long.toString(epoch));
                            check.content("$.[?(@.service=='perfectsec_attacked')].data.key1", attacked[0]);
                            check.content("$.[?(@.service=='perfectsec_attacked')].data.key2", attacked[1]);
                            check.content("$.[?(@.service=='perfectsec_attacked')].data.key3", attacked[2]);
                        }
                    }
                } else {
                    check.missing("$.[?(@.service=='perfectsec')].data");
                    check.missing("$.[?(@.service=='perfectsec_attacked')].data");
                }
            };

            getAndCheck(s, "/data", "GET DATA", callback);

            if (LocalDateTime.now().isAfter(stopAt))
                break;

            while (true) {
                if (LocalDateTime.now().isAfter(stopAt))
                    break;
                if (random.nextInt(4) == 0) { // 25%
                    // modify session data
                }

                getAndCheck(s, "/data", "GET DATAx", callback);

                if (LocalDateTime.now().isAfter(stopAt))
                    break;
                sleep(800);
            }
        }
    }
}
