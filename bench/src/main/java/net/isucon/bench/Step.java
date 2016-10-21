package net.isucon.bench;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.jetty.client.HttpClient;

public class Step {
    private long stepHardTimeout;
    private ArrayList<Scenario> list;
    private ArrayList<Result> results;
    private ArrayList<Thread> threads;

    private long STEP_WATCHER_INTERVAL_MS = 200;
    private long TIMEOUT_ADDITIONAL = 5000;

    public Step(Class<? extends Scenario>... klasses) {
        this.list = new ArrayList<Scenario>();
        long maxHardTimeout = 0;
        for (Class<? extends Scenario> klass : klasses) {
            try {
                Scenario item = klass.getConstructor().newInstance();
                list.add(item);
                if (maxHardTimeout < item.getHardTimeout())
                    maxHardTimeout = item.getHardTimeout();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                System.err.format("Failed to create instance of Scenario: %s%n", klass);
                System.exit(1);
            }
        }
        this.stepHardTimeout = maxHardTimeout + TIMEOUT_ADDITIONAL;
        this.results = new ArrayList<Result>();
    }

    public String name() {
        ArrayList<String> names = new ArrayList<String>();
        for (Scenario s : list) {
            names.add(s.name());
        }
        return "{" + String.join(",", names) + "}";
    }

    public void execute(HttpClient client, Config config, List<Session> sessions) {
        LocalDateTime started = LocalDateTime.now();
        threads = new ArrayList<Thread>();
        if (sessions.size() < list.size()) {
            String msg = String.format("Scenario number in a step is grater than input data size: %d > %d", list.size(), sessions.size());
            throw new RuntimeException(msg);
        }
        Collections.shuffle(sessions);
        int sessionsPerScenario = sessions.size() / list.size();
        int now = 0;
        for (Scenario sc : list) {
            List<Session> chunk = sessions.subList(now, now + sessionsPerScenario);
            now += sessionsPerScenario;
            Runnable task = () -> {
                try {
                    sc.setHttpClient(client);
                    Result r = sc.execute(config, chunk);
                    synchronized(results){
                        results.add(r);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    Result re = generateIncompleteResult("EXCEPTION", started.until(LocalDateTime.now(), ChronoUnit.MILLIS));
                    results.add(re);
                }
            };

            Thread t = new Thread(task);
            threads.add(t);
            t.start();
        }
    }

    public void join() {
        LocalDateTime started = LocalDateTime.now();
        LocalDateTime timeoutAt = started.plus(stepHardTimeout, ChronoUnit.MILLIS);
        while (timeoutAt.isAfter(LocalDateTime.now())) {
            boolean completed = true;
            boolean interrupted = false;
            for (Thread t : threads) {
                try {
                    t.join(STEP_WATCHER_INTERVAL_MS);
                } catch (InterruptedException e) {
                    // just ignore: next loop will come if thread is still alive
                }
                if (t.isAlive()) {
                    completed = false;
                }
            }
            if (completed)
                break;
        }
    }

    public void kill() {
        for (Thread t : threads) {
            try {
                t.join(1); // ms
            } catch (InterruptedException e) {
                // just ignore
            }
            t.interrupt();
        }
    }

    public boolean isFinished() {
        synchronized(results) {
            return list.size() == results.size();
        }
    }

    public Result mergedResult() {
        synchronized(results) {
            return Result.merge(name(), Result.MergeType.PARALLEL, results);
        }
    }

    private Result generateIncompleteResult(String label, long timeoutElapsed) {
        Result r = new Result(false, timeoutElapsed);
        r.addViolation(label, "A scenario in parallel didn't finish");
        return r;
    }
}
