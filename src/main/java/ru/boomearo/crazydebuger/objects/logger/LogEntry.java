package ru.boomearo.crazydebuger.objects.logger;

import lombok.Value;

@Value
public class LogEntry {
    long time;
    LogLevel logLevel;
    String message;
}
