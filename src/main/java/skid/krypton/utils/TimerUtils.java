package skid.krypton.utils;

public class TimerUtils {
    private long lastTime;

    public TimerUtils() {
        this.lastTime = System.currentTimeMillis();
    }

    public boolean delay(long delay) {
        return System.currentTimeMillis() - lastTime >= delay;
    }

    public void reset() {
        this.lastTime = System.currentTimeMillis();
    }

    public long getTime() {
        return System.currentTimeMillis() - lastTime;
    }
} 