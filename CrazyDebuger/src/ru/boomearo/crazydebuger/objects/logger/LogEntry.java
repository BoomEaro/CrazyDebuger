package ru.boomearo.crazydebuger.objects.logger;

public class LogEntry {

    private final long time;
    private final LogLevel level;
    private final String msg;
    
    public LogEntry(long time, LogLevel level, String msg) {
        this.time = time;
        this.level = level;
        this.msg = msg;
    }
    
    public long getTime() {
        return this.time;
    }
    
    public LogLevel getLogLevel() {
        return this.level;
    }
    
    public String getMessage() {
        return this.msg;
    }
    
}
