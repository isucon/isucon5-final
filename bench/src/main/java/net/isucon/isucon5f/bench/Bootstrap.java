package net.isucon.isucon5f.bench;

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

import net.isucon.bench.Scenario;
import net.isucon.bench.Step;
import net.isucon.bench.Result;
import net.isucon.bench.Parameter;
import net.isucon.bench.Session;

public class Bootstrap extends Base {
    public Bootstrap(Long timeout) {
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
                    check.isContentBodyChecksum("c2f4ae15ab0c59e0d75a06e031550ad70e59dfc2");
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
            {
                HashMap<String,String> form = new HashMap<String,String>();
                form.put("service", "surname");
                form.put("param_name", "q");
                form.put("param_value", I5FSurnames.getQuery(param.subscriptions.get("surname").params.get("q")));
                postAndCheck(session, "/modify", form, "MODIFY GIVENNAME", (check) -> {
                        check.isRedirect("/modify");
                    });
            }

            if (grade.equals("small") || grade.equals("standard") || grade.equals("premium")){
                HashMap<String,String> form = new HashMap<String,String>();
                form.put("service", "givenname");
                form.put("param_name", "q");
                form.put("param_value", I5FGivennames.getQuery(param.subscriptions.get("givenname").params.get("q")));
                postAndCheck(session, "/modify", form, "MODIFY GIVENNAME", (check) -> {
                        check.isRedirect("/modify");
                    });
            }

            if (grade.equals("standard") || grade.equals("premium")){
                // tenki ?
            }

            if (grade.equals("premium")){
                // modify perfectsec
                // modify perfectsec_attacked
            }
        }

        {
            I5FParameter param = (I5FParameter) session.param();
            String grade = param.grade;
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
                    check.contentMatch("$.[?(@.service=='ken')].data.addresses.*", kenValue);

                    check.exist("$.[?(@.service=='ken2')]", 1);
                    check.contentMatch("$.[?(@.service=='ken2')].data.addresses.*", ken2Value);

                    check.exist("$.[?(@.service=='surname')].data.query", 1);
                    {
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
                        String qKey = param.subscriptions.get("givenname").params.get("q");
                        String query = I5FGivennames.getQuery(qKey);
                        String queryCheck = String.format("$.[?(@.service=='givenname')].data.[?(@.query=='%s')]", query);
                        check.exist(queryCheck, 1);
                        List<I5FJsonData.NameElement> result = I5FGivennames.getResult(qKey);
                        for (I5FJsonData.NameElement r : result) {
                            check.contentMatch("$.[?(@.service=='givenname')].data.result..name", r.name);
                        }
                    }

                    if (grade.equals("standard") || grade.equals("premium")){
                    }

                    if (grade.equals("premium")){
                    }
                });
        }
    }
}
/*
[
{"service":"ken","data":{"zipcode":"6900014","addresses":["島根県 松江市 八雲台"]}},
{"service":"ken2","data":{"zipcode":"1530042","addresses":["東京都 目黒区 青葉台", "東京都 目黒区 テストさん"]}},
{"service":"surname","data":{"query":"神","result":[
  {"yomi":"カクミ","name":"神代"},{"yomi":"カグラオカ","name":"神楽岡"},
  {"yomi":"カゴシマ","name":"神子島"},{"yomi":"カジロ","name":"神代"},
  {"yomi":"カジロ","name":"神白"},{"yomi":"カナガワ","name":"神奈川"},
  ]}},
{"service":"givenname","data":{"query":"さと","result":[
  {"yomi":"サト","name":"郷"},{"yomi":"サト","name":"郷寧"},
  {"yomi":"サト","name":"慧"},{"yomi":"サト","name":"佐音"},
  {"yomi":"サト","name":"佐都"},{"yomi":"サト","name":"佐橙"},
  ]}},
{"service":"perfectsec",
 "data":{
  "req":"ps1",
  "key":"kPOHq448HawFt24ihg8l",
  "onetime_token":"38c56517174a3a304f888f140d504674ab3c346c"}},
{"service":"perfectsec_attacked",
 "data":{
  "key1":"0516f6726860dac017136f8fadbda9f9e084d07b",
  "key2":"2d383c91ae1ecb552b773052145b052782e39b73",
  "key3":"fc6004fdc6de41d62852fc0249af0f473769d0e6",
  "updated_at":1445948187}}]
*/
