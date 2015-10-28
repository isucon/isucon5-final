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
    private static final String PARAMETER_CLASS = "net.isucon.isucon5f.bench.I5FParameter";

    public Bootstrap(Long timeout) {
        super(timeout);
    }

    @Override
    public String parameterClassName() {
        return PARAMETER_CLASS;
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

    private static String[][] BOOTSTRAP_TEST_DATA = new String[][]{
        { "tony1@moris.io", "tonyny31", "micro" },
        { "tony2@moris.io", "tonyny32", "small" },
        { "tony3@moris.io", "tonyny33", "standard" },
        { "tony4@moris.io", "tonyny34", "premium" },
    };

    private static Session testData(int index) {
        I5FParameter param = new I5FParameter();
        String[] data = BOOTSTRAP_TEST_DATA[index];
        param.email = data[0];
        param.password = data[1];
        param.grade = data[2];

        param.putDummySubscriptions();

        return new Session(param);
    }

    @Override
    public void scenario(List<Session> originalSessions) {
        for (int i = 0 ; i < 4 ; i++) {
            scenarioOnce(testData(i));
        }
    }

    private void scenarioOnce(Session session) {
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
                    check.isContentBodyChecksum("2a7e762957979ed3b2bf7ba3503a471b8ed76437");
                });
            String intervalVal = null;
            switch (param.grade) {
            case "micro":    intervalVal = "30000"; break;
            case "small":    intervalVal = "30000"; break;
            case "standard": intervalVal = "20000"; break;
            case "premium":  intervalVal = "10000"; break;
            }
            final String interval = intervalVal;
            getAndCheck(session, "/user.js", "USER JS", (check) -> {
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
                HashMap<String,String> form = new HashMap<String,String>();
                form.put("service", "tenki");
                form.put("token", param.subscriptions.get("tenki").token);
                postAndCheck(session, "/modify", form, "MODIFY TENKI", (check) -> {
                        check.isRedirect("/modify");
                    });
            }

            if (grade.equals("premium")){
                HashMap<String,String> form1 = new HashMap<String,String>();
                form1.put("service", "perfectsec");
                form1.put("param_name", "req");
                form1.put("param_value", param.subscriptions.get("perfectsec").params.get("req"));
                form1.put("token", param.subscriptions.get("perfectsec").token);
                postAndCheck(session, "/modify", form1, "MODIFY PERFECTSEC", (check) -> {
                        check.isRedirect("/modify");
                    });

                HashMap<String,String> form2 = new HashMap<String,String>();
                form2.put("service", "perfectsec_attacked");
                form2.put("token", param.subscriptions.get("perfectsec").token);
                postAndCheck(session, "/modify", form2, "MODIFY PERFECTSEC_ATTACKED", (check) -> {
                        check.isRedirect("/modify");
                    });
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
                    } else {
                        check.missing("$.[?(@.service=='givenname')].data.query");
                    }

                    if (grade.equals("standard") || grade.equals("premium")){
                        check.exist("$.[?(@.service=='tenki')].data", 1);
                        String date = (String) check.find("$.[?(@.service=='tenki')].data.date").get(0);
                        // At Check, add addViolation for expired data
                        String yoho = I5FTenki.getYoho(date);
                        check.content("$.[?(@.service=='tenki')].data.yoho", yoho);
                    } else {
                        check.missing("$.[?(@.service=='tenki')].data");
                    }

                    if (grade.equals("premium")){
                        check.exist("$.[?(@.service=='perfectsec')].data", 1);
                        String req = param.subscriptions.get("perfectsec").params.get("req");
                        check.content("$.[?(@.service=='perfectsec')].data.req", req);

                        String token = param.subscriptions.get("perfectsec").token;
                        check.exist("$.[?(@.service=='perfectsec')].data.key", 1);
                        String key = (String) check.find("$.[?(@.service=='perfectsec')].data.key").get(0);

                        String onetime = I5FPerfectSecurity.getOneTime(token, req, key);
                        check.content("$.[?(@.service=='perfectsec')].data.onetime_token", onetime);

                        check.exist("$.[?(@.service=='perfectsec_attacked')].data.updated_at", 1);
                        String epoch = String.valueOf(check.find("$.[?(@.service=='perfectsec_attacked')].data.updated_at").get(0));
                        // At Check, add addViolation for expired data
                        String[] attacked = I5FPerfectSecurity.getAttacked(token, epoch);
                        check.content("$.[?(@.service=='perfectsec_attacked')].data.key1", attacked[0]);
                        check.content("$.[?(@.service=='perfectsec_attacked')].data.key2", attacked[1]);
                        check.content("$.[?(@.service=='perfectsec_attacked')].data.key3", attacked[2]);
                    } else {
                        check.missing("$.[?(@.service=='perfectsec')].data");
                        check.missing("$.[?(@.service=='perfectsec_attacked')].data");
                    }
                });
        }
    }
}
/*
[
 {"service":"ken","data":{"zipcode":"0791111","addresses":["北海道 赤平市 若木町北"]}},
 {"service":"ken2","data":{"zipcode":"5008157","addresses":["岐阜県 岐阜市 五坪","岐阜県 岐阜市 五坪町"]}},
 {"service":"surname","data":{"query":"かご","result":[
   {"yomi":"カゴ","name":"加後"},{"yomi":"カゴ","name":"加護"},{"yomi":"カゴ","name":"篭"},{"yomi":"カゴ","name":"籠"},{"yomi":"カゴイ","name":"籠居"},
   {"yomi":"カゴイケ","name":"籠池"},{"yomi":"カゴウラ","name":"籠浦"},{"yomi":"カゴオ","name":"篭尾"},{"yomi":"カゴオ","name":"籠尾"},
   {"yomi":"カゴクラ","name":"籠倉"},{"yomi":"カゴサキ","name":"籠崎"},{"yomi":"カゴシマ","name":"鹿子島"},{"yomi":"カゴシマ","name":"鹿子嶋"},
   {"yomi":"カゴシマ","name":"鹿児島"},{"yomi":"カゴシマ","name":"神子島"},{"yomi":"カゴシマ","name":"篭島"},{"yomi":"カゴシマ","name":"籠島"},
   {"yomi":"カゴセ","name":"籠瀬"},{"yomi":"カゴタニ","name":"篭谷"},{"yomi":"カゴタニ","name":"籠谷"},{"yomi":"カゴノ","name":"加護野"},
   {"yomi":"カゴハシ","name":"篭橋"},{"yomi":"カゴハシ","name":"籠橋"},{"yomi":"カゴハラ","name":"篭原"},{"yomi":"カゴバヤシ","name":"籠林"},
   {"yomi":"カゴミヤ","name":"籠宮"},{"yomi":"カゴヤ","name":"籠谷"},{"yomi":"カゴヤマ","name":"篭山"},{"yomi":"カゴロク","name":"賀籠六"},
   {"yomi":"カゴロク","name":"鹿籠六"}
   ]}},
 {"service":"givenname","data":{"query":"のりしげ","result":[
   {"yomi":"ノリシゲ","name":"紀重"},{"yomi":"ノリシゲ","name":"紀繁"},{"yomi":"ノリシゲ","name":"憲薫"},{"yomi":"ノリシゲ","name":"憲滋"},
   {"yomi":"ノリシゲ","name":"憲重"},{"yomi":"ノリシゲ","name":"憲成"},{"yomi":"ノリシゲ","name":"憲繁"},{"yomi":"ノリシゲ","name":"憲茂"},
   {"yomi":"ノリシゲ","name":"則重"},{"yomi":"ノリシゲ","name":"典茂"},{"yomi":"ノリシゲ","name":"法滋"},{"yomi":"ノリシゲ","name":"法重"},
   {"yomi":"ノリシゲ","name":"法繁"}
   ]}},
 {"service":"tenki","data":{"yoho":"雨のち雷雨","date":"Wed, 28 Oct 2015 16:46:30 JST"}},
 {"service":"perfectsec","data":{"req":"ultimate","key":"qOMaDBClXZ38SUGYJ4Ke","onetime_token":"b5a72de90611837b1efaf430f6efa3f31d6d6fcf"}},
 {"service":"perfectsec_attacked","data":{
   "key1":"c9ab24fb3859e3099973b41a5b55bfbecd988413",
   "key2":"81538bea420f3ed5327b6b003692758cdd8de840",
   "key3":"f28aaf75fecb186a976bd5062cdb67b2d8cd30de",
   "updated_at":1446018374}}
]
*/
