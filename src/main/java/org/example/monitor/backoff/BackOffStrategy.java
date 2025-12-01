package org.example.monitor.backoff;

public class BackOffStrategy {

    private int currentSeconds = 1;
    private final int maxSeconds;

    public BackOffStrategy(int maxSeconds) {
        this.maxSeconds = maxSeconds;
    }

    public int nextDelay(){
        int delay = currentSeconds;
        currentSeconds = Math.max(currentSeconds*2, maxSeconds);
        return delay;
    }

    public void reset(){
        currentSeconds = 1;
    }
}
