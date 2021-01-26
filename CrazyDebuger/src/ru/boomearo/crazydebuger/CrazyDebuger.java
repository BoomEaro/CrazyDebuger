package ru.boomearo.crazydebuger;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.utils.NumberUtil;

import ru.boomearo.crazydebuger.listeners.DeathListener;
import ru.boomearo.crazydebuger.listeners.MainListener;
import ru.boomearo.crazydebuger.runnable.SaveTimer;
import ru.boomearo.crazydebuger.runnable.ZipRunner;
import ru.boomearo.crazydebuger.utils.Ziping;

public class CrazyDebuger extends JavaPlugin {
    private IMoney money = null;

    private SaveTimer timer = null;
    private ZipRunner ziper = null;

    private volatile boolean moneyEnabled = true;
    private volatile boolean deathEnabled = true;
    private volatile boolean runArchiveCheckTimer = true;
    private volatile boolean itemEnabled = false;

    private volatile boolean isReady = false;

    private volatile Long lastZip = null;

    public void onEnable() {
        instance = this;

        File configFile = new File(getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            getLogger().info("Конфиг не найден, создаю новый..");
            saveDefaultConfig();
        }

        loadConfig();

        checkOldDir();
        checkOutdateFiles();

        loadMoneyEss();

        if (this.timer == null) {
            this.timer = new SaveTimer(this);
            this.timer.setPriority(3);
            this.timer.start();
        }
        if (this.runArchiveCheckTimer) {
            if (this.ziper == null) {
                this.ziper = new ZipRunner();
                this.ziper.setPriority(3);
                this.ziper.start();
            }
        }

        getServer().getPluginManager().registerEvents(new MainListener(), this);

        if (this.deathEnabled) {
            getServer().getPluginManager().registerEvents(new DeathListener(), this);
        }

        getLogger().info("Плагин успешно включен.");

    }
    public void onDisable() {
        //Сохраняем оставшиеся задачи
        this.timer.save();

        //Завершаем таймеры
        this.timer.interrupt();
        if (this.ziper != null) {
            this.ziper.interrupt();
        }

        getLogger().info("Плагин успешно выключен.");
    }

    private static CrazyDebuger instance = null;
    public static CrazyDebuger getInstance() { 
        if (instance != null) return instance; return null; 
    }

    public void loadConfig() {
        this.moneyEnabled = getConfig().getBoolean("Configuration.moneyEnabled");
        this.deathEnabled = getConfig().getBoolean("Configuration.deathEnabled");
        this.runArchiveCheckTimer = getConfig().getBoolean("Configuration.runArchiveCheckTimer");
        this.itemEnabled = getConfig().getBoolean("Configuration.itemEnabled");
        
        this.lastZip = getConfig().getLong("LastZip");
    }

    private void loadMoneyEss() {
        Plugin tmpPl = Bukkit.getPluginManager().getPlugin("Essentials");
        if (tmpPl != null && this.moneyEnabled) {
            this.money = new GettingMoneyLegal(tmpPl);
        }
        else {
            this.money = new GettingMoneyEmpty();
        }
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

    public static String craftMainMsg(String java_date, String ip, String moneys, double x, double y, double z, String world, String pName) {
        DecimalFormat df = new DecimalFormat("#.##");
        return "[" + java_date + "][" + (ip != null ? "{" + ip + "}" : "") + (moneys != null ? "( " + moneys + " " : "") + "(" + df.format(x) + "|" + df.format(y) + "|" + df.format(z) + "|" + world + ")" + ") " + pName + "]: ";
    }

    public static void sendLogMessage(Player player, String info, boolean isAction) {
        CrazyDebuger cd = CrazyDebuger.getInstance();
        if (!cd.isReady()) {
            cd.getLogger().severe("Предотвращена попытка отправки в лог сообщения. Плагин не готов!");
            return;
        }
        long time = System.currentTimeMillis();

        String pName = player.getName();
        String ip = player.getAddress().getHostString();

        Location loc = player.getLocation();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        String world = loc.getWorld().getName();

        //Если этот метод был вызван в основном потоке, то выполняем в другом потоке, в противном случае выполняем в этом же потоке
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(cd, () -> {
                sendThreadPlayer(time, pName, ip, x, y, z, world, info, isAction);
            });
            return;
        }

        sendThreadPlayer(time, pName, ip, x, y, z, world, info, isAction);
    }
    
    private static void sendThreadPlayer(long time, String name, String ip, double x, double y, double z, String world, String info, boolean isAction) {
        Date date = new Date(time); 
        SimpleDateFormat jdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        String java_date = jdf.format(date);

        String money = CrazyDebuger.getInstance().getMoney(name);

        String msg = craftMainMsg(java_date, ip, money, 
                x, y, z, world, name) + (isAction ? "== " + ChatColor.stripColor(info.replace("\n", " ")) + " ==\n" : ChatColor.stripColor(info.replace("\n", " ")) + "\n");

        CrazyDebuger.getInstance().getSaveTimer().addLog(name, msg);
    }


    //ess support
    private class GettingMoneyEmpty implements IMoney {
        @Override
        public String getMoney(String user) {
            return null;
        }

    }

    public void checkOutdateFiles() { 
        this.isReady = false;

        if (this.lastZip == null) {
            this.isReady = true;
            return;
        }

        //Каждый месяц будет работать архивирование в фоне.
        if (((System.currentTimeMillis() - this.lastZip) / 1000) <= 2419200) {
            this.isReady = true;
            return;
        }

        this.getLogger().info("Начинаю архивирование всех логов в фоне.. ");

        long start = System.currentTimeMillis();

        this.getConfig().set("LastZip", start);
        this.lastZip = start;

        this.saveConfig();

        Date date = new Date(System.currentTimeMillis()); 
        SimpleDateFormat jdf = new SimpleDateFormat("HH-mm-ss   dd.MM.yyyy");
        String java_date = jdf.format(date);


        File plSource = new File(getDataFolder() + "/players/latest/");
        plSource.getParentFile().mkdirs();
        File plNew = new File(getDataFolder() + "/players/old/players-" + java_date + ".zip");
        plNew.getParentFile().mkdirs();

        File[] files = plSource.listFiles();

        if (files != null) {
            if (files.length > 0) {
                try {
                    Ziping.zipDir(plSource, plNew);
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
        }

        this.getLogger().info("Архивирование отдельных игроков завершено.");

        File mainSource = new File(getDataFolder() + "/general/latest.log");
        mainSource.getParentFile().mkdirs();
        File mainNew = new File(getDataFolder() + "/general/old/general-" + java_date + ".zip");
        mainNew.getParentFile().mkdirs();

        if (mainSource.exists()) {
            Ziping.zipFile(mainSource, mainNew);

            mainSource.delete();
        }

        this.getLogger().info("Архивирование главного лога завершено.");
        long end = System.currentTimeMillis();

        this.getLogger().info("Полное архивированое логов успешно завершено за " + (end - start) + "мс.");

        this.isReady = true;
    }

    private void checkOldDir() {
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
    }

    private static class GettingMoneyLegal implements IMoney {
        private Essentials ess;

        public GettingMoneyLegal(Plugin pl) {
            this.ess = (Essentials) pl;
        }

        @Override
        public String getMoney(String user) {
            BigDecimal bd = this.ess.getUser(user).getMoney();
            return NumberUtil.displayCurrency(bd, this.ess);
        }

    }

    private static interface IMoney {

        public String getMoney(String user);

    }
    //

    public SaveTimer getSaveTimer() {
        return this.timer;
    }

    public boolean isReady() {
        return this.isReady;
    }

}
