package ru.boomearo.crazydebuger;

import java.io.File;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import ru.boomearo.crazydebuger.listeners.DeathListener;
import ru.boomearo.crazydebuger.listeners.ItemListener;
import ru.boomearo.crazydebuger.listeners.MainListener;
import ru.boomearo.crazydebuger.managers.LoggerManager;

@Getter
public class CrazyDebuger extends JavaPlugin {

    private LoggerManager loggerManager = null;

    private static CrazyDebuger instance = null;

    @Override
    public void onEnable() {
        instance = this;

        File configFile = new File(getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            getLogger().info("Конфиг не найден, создаю новый..");
            saveDefaultConfig();
        }

        if (this.loggerManager == null) {
            this.loggerManager = new LoggerManager();

            this.loggerManager.load(getConfig());
        }

        getServer().getPluginManager().registerEvents(new MainListener(this.loggerManager), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this.loggerManager), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this.loggerManager), this);

        getLogger().info("Плагин успешно включен.");

    }

    @Override
    public void onDisable() {
        if (this.loggerManager != null) {
            this.loggerManager.unload();
        }

        getLogger().info("Плагин успешно выключен.");
    }

    public static CrazyDebuger getInstance() {
        return instance;
    }

}
