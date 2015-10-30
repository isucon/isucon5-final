package net.isucon.isucon5f.bench;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

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

    public Load(Long timeout) {
        super(timeout);
    }

    @Override
    public String parameterClassName() {
        return PARAMETER_CLASS;
    }

    @Override
    public void scenario(List<Session> sessions) {
        System.err.println("Load");
        Random random = new Random();

        LocalDateTime stopAt = LocalDateTime.now().plus(DURATION_MILLIS, ChronoUnit.MILLIS);

        while (true) {
            if (LocalDateTime.now().isAfter(stopAt))
                break;
            Session s = sessions.get(random.nextInt((int) sessions.size()));

            get(s, "/login");
            get(s, "/css/bootstrap.min.css");
            get(s, "/css/signin.css");
            post(s, "/login", formLogin(s));

            if (LocalDateTime.now().isAfter(stopAt))
                break;

            get(s, "/");
            get(s, "/css/bootstrap.min.css");
            get(s, "/css/jumbotron-narrow.css");
            get(s, "/js/jquery-1.11.3.js");
            get(s, "/js/bootstrap.js");
            get(s, "/js/airisu.js");
            get(s, "/user.js");

            if (LocalDateTime.now().isAfter(stopAt))
                break;

            while (true) {
                get(s, "/data");
                if (LocalDateTime.now().isAfter(stopAt))
                    break;
            }
        }
    }

    // TODO: subscenario w/ specified concurrency & subscenario-specific state
}
