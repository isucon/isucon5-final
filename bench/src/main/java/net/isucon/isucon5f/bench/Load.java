package net.isucon.isucon5f.bench;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.net.URI;
import java.net.URISyntaxException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ChronoField;

import net.isucon.bench.Scenario;
import net.isucon.bench.Step;
import net.isucon.bench.Result;
import net.isucon.bench.Parameter;
import net.isucon.bench.Session;

public class Load extends Base {
    private static final String PARAMETER_CLASS = "net.isucon.isucon5f.bench.I5FParameter";

    private static long DURATION_MILLIS = 60 * 1000;

    public Load() {
        super(DURATION_MILLIS);
    }

    @Override
    public String parameterClassName() {
        return PARAMETER_CLASS;
    }

    @Override
    public void scenario(List<Session> sessions) {
        System.err.println("Load");

        while (true) {
            stopCheck();
            Session s = pick(sessions);

            get(s, "/login");
            get(s, "/css/bootstrap.min.css");
            get(s, "/css/signin.css");
            post(s, "/login", formLogin(s));

            stopCheck();

            get(s, "/");
            get(s, "/css/bootstrap.min.css");
            get(s, "/css/jumbotron-narrow.css");
            get(s, "/js/jquery-1.11.3.js");
            get(s, "/js/bootstrap.js");
            get(s, "/js/airisu.js");
            get(s, "/user.js");

            stopCheck();

            for (int i = 0 ; i < 10 ; i++) {
                get(s, "/data");

                stopCheck();
            }
        }
    }
}
