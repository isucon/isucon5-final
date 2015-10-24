package net.isucon.bench;

public class State {
    private boolean running;

    public State() {
        this.running = true;
    }

    public void init() {
        this.running = true;
    }

    public boolean isRunning() {
        return running;
    }

    public void finish() {
        this.running = false;
    }
}
