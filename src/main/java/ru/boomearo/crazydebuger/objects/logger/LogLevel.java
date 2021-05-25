package ru.boomearo.crazydebuger.objects.logger;

public enum LogLevel {

    INFO("Информация"),
    WARNING("Предупреждение"),
    SEVERE("Ошибка");

    private final String desc;

    LogLevel(String desc) {
        this.desc = desc;
    }

    public String getDescription() {
        return this.desc;
    }
}
