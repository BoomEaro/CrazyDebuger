package ru.boomearo.crazydebuger.objects.logger;

import lombok.Builder;
import lombok.Value;

import org.bukkit.entity.Entity;

@Value
@Builder
public class LogMessage {

    Entity entity;
    String textAction;
    boolean action;
    LogLevel logLevel;

}
