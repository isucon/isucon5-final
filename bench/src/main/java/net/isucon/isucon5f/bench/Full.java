package net.isucon.isucon5f.bench;

import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import net.isucon.bench.Scenario;
import net.isucon.bench.Step;
import net.isucon.bench.Result;
import net.isucon.bench.Parameter;
import net.isucon.bench.Session;

public class Full extends Scenario {
    private static final String PARAMETER_CLASS = "net.isucon.isucon5f.bench.I5FParameter";

    private static long DURATION_MILLIS = 120 * 1000;

    public Full(Long timeout) {
        super(timeout);
    }

    @Override
    public String parameterClassName() {
        return PARAMETER_CLASS;
    }

    @Override
    public boolean complex() {
        return true;
    }

    @Override
    public Result finishHook(Result result) {
        /*
        long requests = result.requests;
        // critical line:  1% for exceptions
        //                 1% for errors
        //                 1% for failures
        if (result.responses.exception * 100.0 / (requests * 1.0) >= 1.0)
            result.addViolation("Too many exceptions", "通信エラー等の失敗が多過ぎます(1%以上)");
        if (result.responses.error * 100.0 / (requests * 1.0) >= 1.0)
            result.addViolation("Too many errors", "ステータス 5xx のレスポンスが多過ぎます(1%以上)");
        if (result.responses.failure * 100.0 / (requests * 1.0) >= 10.0)
            result.addViolation("Too many failures", "ステータス 4xx のレスポンスが多過ぎます(10%以上)");

        if (result.violations.size() > 0) {
            int num = 0;
            for (Result.Violation v : result.violations) {
                if (! v.description.equals("アプリケーションが 3000 ミリ秒以内に応答しませんでした"))
                    num += 1;
            }
            if (num > 0)
                result.fail();
        }
        */
        return result;
    }

    @Override
    public Step[] steps() {
        Step[] steps = new Step[3];
        steps[0] = new Step(35000L, Init.class);
        steps[1] = new Step(60000L, Bootstrap.class);
        // steps[2] = new Step(70000L, Checker.class, Load.class, Load.class, Load.class, Load.class);
        steps[2] = new Step(70000L, Checker.class, Load.class);

        return steps;
    }
}
