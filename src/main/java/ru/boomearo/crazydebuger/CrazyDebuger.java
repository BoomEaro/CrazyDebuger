package ru.boomearo.crazydebuger;

import java.io.File;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import ru.boomearo.crazydebuger.listeners.DeathListener;
import ru.boomearo.crazydebuger.listeners.ItemListener;
import ru.boomearo.crazydebuger.listeners.MainListener;
import ru.boomearo.crazydebuger.objects.IVault;
import ru.boomearo.crazydebuger.objects.logger.LogEntry;
import ru.boomearo.crazydebuger.objects.logger.LogLevel;
import ru.boomearo.crazydebuger.runnable.SaveTimer;
import ru.boomearo.crazydebuger.utils.Ziping;
import ru.boomearo.crazydebuger.utils.NumberUtils;

public class CrazyDebuger extends JavaPlugin {

    private IVault vault = null;

    private SaveTimer timer = null;

    private volatile boolean moneyEnabled = true;
    private volatile boolean deathEnabled = true;
    private volatile boolean itemEnabled = false;

    private volatile boolean ready = false;

    private volatile long lastZip = 0;

    private static CrazyDebuger instance = null;

    //Месяц
    private static final long zipTime = 2419200;

    @Override
    public void onEnable() {
        instance = this;

        File configFile = new File(getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            getLogger().info("Конфиг не найден, создаю новый..");
            saveDefaultConfig();
        }

        loadConfig();

        loadVault();

        Thread thread = new Thread(this::checkOutdateFiles);

        thread.setName("CheckOutdatedFiles-Thread");
        thread.setPriority(3);
        thread.start();


        if (this.timer == null) {
            this.timer = new SaveTimer();
            this.timer.setPriority(3);
            this.timer.start();
        }

        getServer().getPluginManager().registerEvents(new MainListener(), this);

        if (this.deathEnabled) {
            getServer().getPluginManager().registerEvents(new DeathListener(), this);
        }

        if (this.itemEnabled) {
            getServer().getPluginManager().registerEvents(new ItemListener(), this);
        }

        getLogger().info("Плагин успешно включен.");

    }

    @Override
    public void onDisable() {
        //Сохраняем оставшиеся задачи
        this.timer.save();

        //Завершаем таймеры
        this.timer.interrupt();

        getLogger().info("Плагин успешно выключен.");
    }

    public static CrazyDebuger getInstance() {
        return instance;
    }

    private void loadVault() {
        //Сначала по умолчанию создает пустой Vault с лямбдой
        this.vault = player -> null;
        try {
            Plugin vaultPl = Bukkit.getPluginManager().getPlugin("Vault");
            //Убеждаемся что Vault на сервере и в конфигурации он включен
            if (vaultPl == null) {
                throw new Exception("Vault не найден.");
            }
            if (!this.moneyEnabled) {
                throw new Exception("Денежный лог отключен в настройках.");
            }

            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (rsp == null) {
                throw new Exception("Сервис Vault не найден.");
            }
            net.milkbowl.vault.economy.Economy econ = rsp.getProvider();
            if (econ == null) {
                throw new Exception("Экономика Vault не найдена.");
            }

            //Создаем лямбду с реализацией
            this.vault = player -> NumberUtils.displayCurrency(econ.getBalance(player));
            this.getLogger().info("Vault успешно привязан!");
        }
        catch (Exception e) {
            this.getLogger().warning("Ошибка привязки Vault: " + e.getMessage());
        }
    }

    public void loadConfig() {
        this.moneyEnabled = getConfig().getBoolean("Configuration.moneyEnabled");
        this.deathEnabled = getConfig().getBoolean("Configuration.deathEnabled");
        this.itemEnabled = getConfig().getBoolean("Configuration.itemEnabled");

        this.lastZip = getConfig().getLong("LastZip");
    }

    public IVault getVault() {
        return vault;
    }

    public static void sendLogMessage(Player player, String info, boolean isAction) {
        long time = System.currentTimeMillis();

        String pName = player.getName();
        InetSocketAddress address = player.getAddress();
        String ip = null;
        if (address != null) {
            ip = address.getHostString();
        }

        Location loc = player.getLocation();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        String world = loc.getWorld().getName();

        String money = CrazyDebuger.getInstance().getVault().getMoney(pName);

        CrazyDebuger.getInstance().getSaveTimer().addLog(pName, new LogEntry(time, LogLevel.INFO, craftMsgLog(ip, money, x, y, z, world, pName, info, isAction)));
    }

    public static String craftMsgLog(String ip, String money, double x, double y, double z, String world, String entity, String info, boolean isAction) {
        DecimalFormat df = new DecimalFormat("#.##");

        return "[" + (ip != null ? "{" + ip + "}" : "") +
                (money != null ? "( " + money + " " : "") +
                "(" + df.format(x) + "|" + df.format(y) + "|" + df.format(z) + "|" + world + ")" +
                ") " + entity + "]: " +
                (isAction ? "== " + ChatColor.stripColor(info.replace("\n", " ")) + " ==" : ChatColor.stripColor(info.replace("\n", " ")));
    }

    public void checkOutdateFiles() {
        this.ready = false;

        long time = ((System.currentTimeMillis() - this.lastZip) / 1000);

        this.getLogger().info("Следующее сохранение логов через: " + (zipTime - time) + " сек.");

        DecimalFormat df = new DecimalFormat("#.##");

        File mainSource = new File(getDataFolder() + "/general/latest.log");
        mainSource.getParentFile().mkdirs();

        double mainLogSize = getFileSizeMegaBytes(mainSource);
        this.getLogger().info("Размер главного лога: " + df.format(mainLogSize) + " MB.");

        File plSource = new File(getDataFolder() + "/players/latest/");
        plSource.getParentFile().mkdirs();

        double playerDirSize = getDirectorySizeMegaBytes(plSource);
        this.getLogger().info("Размер директории игроков: " + df.format(playerDirSize) + " MB.");

        //Если прошел месяц то запускаем
        //Если размер директории или главного лога привышает 150 мб, то запускаем архивирование все равно.
        if (time > zipTime || mainLogSize > 150 || playerDirSize > 150) {

            try {
                runArchive(mainSource, plSource);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.ready = true;
            }
            return;
        }

        this.ready = true;
    }

    private void runArchive(File mainSource, File plSource) {
        this.getLogger().info("Начинаю архивирование всех логов.. ");

        long start = System.currentTimeMillis();

        this.getConfig().set("LastZip", start);
        this.lastZip = start;

        this.saveConfig();

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat jdf = new SimpleDateFormat("HH-mm-ss   dd.MM.yyyy");
        String java_date = jdf.format(date);

        File plNew = new File(getDataFolder() + "/players/old/players-" + java_date + ".zip");
        plNew.getParentFile().mkdirs();

        File[] files = plSource.listFiles();

        if (files != null) {
            try {
                Ziping.zipDir(plSource, plNew, false);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            for (File f : files) {
                if (f.isFile()) {
                    f.delete();
                }
            }
        }

        this.getLogger().info("Архивирование отдельных игроков завершено.");

        File mainNew = new File(getDataFolder() + "/general/old/general-" + java_date + ".zip");
        mainNew.getParentFile().mkdirs();

        if (mainSource.exists()) {
            Ziping.zipFile(mainSource, mainNew);

            mainSource.delete();
        }

        this.getLogger().info("Архивирование главного лога завершено.");
        long end = System.currentTimeMillis();

        this.getLogger().info("Полное архивированое логов успешно завершено за " + (end - start) + "мс.");
    }

    private static double getDirectorySizeMegaBytes(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size = size + file.length();
                }
            }
        }

        return (double) size / (1024 * 1024);
    }

    private static double getFileSizeMegaBytes(File file) {
        return (double) file.length() / (1024 * 1024);
    }

    public SaveTimer getSaveTimer() {
        return this.timer;
    }

    public boolean isReady() {
        return this.ready;
    }

    @SuppressWarnings("deprecation")
    public static String getNormalizedItemName(ItemStack item) {
        return item.getType() + (item.getDurability() > 0 ? ":" + item.getDurability() : "") + "(" + item.getAmount() + ")";
    }

}
