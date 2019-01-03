package net.arksea.zipkin.akka;

public class TimerImpl implements Timer {


    public long nowNano() {
        return System.currentTimeMillis() * 1000_000L;
    }

    public long nowMicro() {
        return System.currentTimeMillis() * 1000L;
    }

    public long nowMilli() {
        return System.currentTimeMillis();
    }
}
