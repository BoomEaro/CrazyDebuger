package ru.boomearo.crazydebuger.objects.logger;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LogLevel {

    INFO("Информация"),
    WARNING("Предупреждение"),
    SEVERE("Ошибка");

    private final String description;
}
