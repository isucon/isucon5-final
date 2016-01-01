package net.isucon.isucon5f.bench;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import java.net.URI;
import java.net.URISyntaxException;

import net.isucon.bench.Scenario;
import net.isucon.bench.Parameter;
import net.isucon.bench.Session;

public class Base extends Scenario {
    private static long DEFAULT_HARD_TIMEOUT_WAIT = 5 * 1000;

    public Base(long softTimeout) {
        super(softTimeout, softTimeout + DEFAULT_HARD_TIMEOUT_WAIT);
    }

    protected Map formLogin(Session session) {
        I5FParameter p = (I5FParameter) session.param();
        Map form = new HashMap();

        form.put("email", p.email);
        form.put("password", p.password);
        return form;
    }
}
