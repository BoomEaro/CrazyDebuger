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
import org.bukkit.plugin.java.JavaPlugin;

import ru.boomearo.crazydebuger.listeners.DeathListener;
import ru.boomearo.crazydebuger.listeners.ItemListener;
import ru.boomearo.crazydebuger.listeners.MainListener;
import ru.boomearo.crazydebuger.objects.essmoney.EmptyMoney;
import ru.boomearo.crazydebuger.objects.essmoney.EssentialsMoney;
import ru.boomearo.crazydebuger.objects.essmoney.IMoney;
import ru.boomearo.crazydebuger.objects.logger.LogEntry;
import ru.boomearo.crazydebuger.objects.logger.LogLevel;
import ru.boomearo.crazydebuger.runnable.SaveTimer;
import ru.boomearo.crazydebuger.utils.Ziping;

public class CrazyDebuger extends JavaPlugin {
    private IMoney money = null;

    private SaveTimer timer = null;

    private volatile boolean moneyEnabled = true;
    private volatile boolean deathEnabled = true;
    private volatile boolean itemEnabled = false;

    private volatile boolean ready = false;

    private volatile long lastZip = 0;
    
    private static CrazyDebuger instance = null;
    
    //Месяц
    private static final long zipTime = 2419200;
    
    public void onEnable() {
        instance = this;

        File configFile = new File(getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            getLogger().info("Конфиг не найден, создаю новый..");
            saveDefaultConfig();
        }

        loadConfig();

        Thread thread = new Thread(() -> {
            //checkOldDir();
            checkOutdateFiles();
        });
        
        thread.setName("CheckOutdatedFiles-Thread");
        thread.setPriority(3);
        thread.start();

        loadMoneyEss();

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

    public void loadConfig() {
        this.moneyEnabled = getConfig().getBoolean("Configuration.moneyEnabled");
        this.deathEnabled = getConfig().getBoolean("Configuration.deathEnabled");
        this.itemEnabled = getConfig().getBoolean("Configuration.itemEnabled");
        
        this.lastZip = getConfig().getLong("LastZip");
    }

    private void loadMoneyEss() {
        Plugin tmpPl = Bukkit.getPluginManager().getPlugin("Essentials");
        if (tmpPl != null && this.moneyEnabled) {
            if (tmpPl instanceof com.earth2me.essentials.Essentials) {
                com.earth2me.essentials.Essentials ess = (com.earth2me.essentials.Essentials) tmpPl;
                this.money = new EssentialsMoney(ess);
                return;
            }
        }
        
        this.money = new EmptyMoney();
    }

    public String getMoney(String name) {
        return this.money.getMoney(name);
    }

    public boolean isMoneyEnabled() {
        return this.moneyEnabled;
    }
    public boolean isDeathEnabled() {
        return this.deathEnabled;
    }
    
    public boolean isItemEnabled() {
        return this.itemEnabled;
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

        String money = CrazyDebuger.getInstance().getMoney(pName);
        
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
    
    /*private void checkOldDir() {
        File plSource = new File(getDataFolder() + "/players/latest/");
        File mainOldSource = new File(getDataFolder() + "/general.log");

        if (!plSource.exists() && mainOldSource.exists()) {
            this.getLogger().info("Обнаружена старая директория.. переносм на новую.");

            File plOldSource = new File(getDataFolder() + "/players/");

            this.getLogger().info("Переносим логи игроков на новую директорию...");


            File plTmp = new File(getDataFolder() + "/playerstmp/");
            plTmp.getParentFile().mkdirs();

            try {
                Files.move(plOldSource.toPath(), plTmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } 
            catch (IOException e) {
                e.printStackTrace();
            }

            plSource.getParentFile().mkdirs();

            try {
                Files.move(plTmp.toPath(), plSource.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            this.getLogger().info("Переносим главный лог на новую директорию...");

            File mainSource = new File(getDataFolder() + "/general/latest.log");
            mainSource.getParentFile().mkdirs();

            try {
                Files.move(mainOldSource.toPath(), mainSource.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }*/


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
