package ru.boomearo.crazydebuger.managers;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import ru.boomearo.crazydebuger.CrazyDebuger;
import ru.boomearo.crazydebuger.objects.PlayerMoneyHandler;
import ru.boomearo.crazydebuger.objects.logger.LogEntry;
import ru.boomearo.crazydebuger.objects.logger.LogLevel;
import ru.boomearo.crazydebuger.objects.logger.LogMessage;
import ru.boomearo.crazydebuger.runnable.SaveTimer;
import ru.boomearo.crazydebuger.utils.NumberUtils;
import ru.boomearo.crazydebuger.utils.Zipping;

import java.io.File;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
public class LoggerManager {

    private PlayerMoneyHandler vault = null;
    private SaveTimer saveTimer = null;

    private boolean moneyEnabled = true;
    private boolean deathEnabled = true;
    private boolean itemEnabled = false;

    private long lastZip = 0;

    //Месяц
    private static final long ZIP_TIME = 2419200;

    public void load(Configuration configuration) {
        loadConfig(configuration);
        loadVault();
        loadSaveTimer();
        loadCheckOutdatedThread();
    }

    private void loadCheckOutdatedThread() {
        //Запускаем при включении плагина одноразовый поток для проверки и архивирования логов
        Thread thread = new Thread(this::checkOutdatedFiles);

        thread.setName("CheckOutdatedFiles-Thread");
        thread.setPriority(3);
        thread.start();
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

            RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (rsp == null) {
                throw new Exception("Сервис Vault не найден.");
            }
            net.milkbowl.vault.economy.Economy econ = rsp.getProvider();
            if (econ == null) {
                throw new Exception("Экономика Vault не найдена.");
            }

            //Создаем лямбду с реализацией
            this.vault = player -> NumberUtils.displayCurrency(econ.getBalance(player));
            CrazyDebuger.getInstance().getLogger().info("Vault успешно привязан!");
        }
        catch (Exception e) {
            CrazyDebuger.getInstance().getLogger().warning("Ошибка привязки Vault: " + e.getMessage());
        }
    }

    public void loadConfig(Configuration configuration) {
        this.moneyEnabled = configuration.getBoolean("Configuration.moneyEnabled");
        this.deathEnabled = configuration.getBoolean("Configuration.deathEnabled");
        this.itemEnabled = configuration.getBoolean("Configuration.itemEnabled");

        this.lastZip = configuration.getLong("LastZip");

        if (this.lastZip <= 0) {
            this.lastZip = System.currentTimeMillis();

            saveLastZip();
        }
    }

    private void loadSaveTimer() {
        if (this.saveTimer == null) {
            this.saveTimer = new SaveTimer();
            this.saveTimer.setPriority(3);
            this.saveTimer.start();
        }
    }

    public void unload() {
        unloadSaveTimer();
    }

    private void unloadSaveTimer() {
        if (this.saveTimer != null) {
            //Сохраняем оставшиеся задачи
            this.saveTimer.save();

            //Завершаем таймеры
            this.saveTimer.interrupt();

            this.saveTimer = null;
        }
    }

    private void saveLastZip() {
        CrazyDebuger.getInstance().getConfig().set("LastZip", this.lastZip);
        CrazyDebuger.getInstance().saveConfig();
    }

    public void sendLogMessage(LogMessage message) {
        Preconditions.checkArgument(message != null);

        long time = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();

        sb.append("[");

        Entity entity = message.getEntity();
        String playerName = null;
        if (entity != null) {
            DecimalFormat df = new DecimalFormat("#.##");

            if (entity instanceof Player) {
                Player player = (Player) entity;

                playerName = player.getName();

                sb.append("{");

                InetSocketAddress address = player.getAddress();
                String ip = null;
                if (address != null) {
                    ip = address.getHostString();
                }

                sb.append(ip);

                sb.append("}");

                String money = this.vault.getMoney(player.getName());
                if (money != null) {
                    sb.append(" (");

                    sb.append(money);

                    sb.append(") ");
                }

             }
            else if (entity instanceof Item) {
                Item item = (Item) entity;

                sb.append("(");

                sb.append(getNormalizedItemName(item.getItemStack()));

                sb.append(")");
            }

            sb.append("(");
            Location loc = entity.getLocation();
            double x = loc.getX();
            sb.append(df.format(x));
            sb.append("|");
            double y = loc.getY();
            sb.append(df.format(y));
            sb.append("|");
            double z = loc.getZ();
            sb.append(df.format(z));
            sb.append("|");
            String world = loc.getWorld().getName();
            sb.append(world);
            sb.append(") ");

            sb.append("name: ");
            sb.append(entity.getName());
        }

        String textAction = message.getTextAction();
        if (textAction != null) {
            sb.append(" ");

            sb.append(ChatColor.stripColor(textAction.replace("\n", "")));
        }

        if (message.isAction()) {
            sb.append(" ");

            sb.append("=+=+=");
        }

        sb.append("]");

        LogLevel logLevel = message.getLogLevel();
        if (logLevel == null) {
            logLevel = LogLevel.INFO;
        }

        this.saveTimer.addLog(playerName, new LogEntry(time, logLevel, sb.toString()));
    }

    public static String getNormalizedItemName(ItemStack item) {
        return item.getType() + (item.getDurability() > 0 ? ":" + item.getDurability() : "") + "(" + item.getAmount() + ")";
    }

    //Именно здесь после успешных проверок таймер будет считаться готовым и будет сохранять на диск логи
    private void checkOutdatedFiles() {
        this.saveTimer.setReady(false);

        long time = ((System.currentTimeMillis() - this.lastZip) / 1000);

        CrazyDebuger.getInstance().getLogger().info("Следующее сохранение логов через: " + (ZIP_TIME - time) + " сек.");

        DecimalFormat df = new DecimalFormat("#.##");

        File mainSource = new File(CrazyDebuger.getInstance().getDataFolder() + "/general/latest.log");
        mainSource.getParentFile().mkdirs();

        double mainLogSize = getFileSizeMegaBytes(mainSource);
        CrazyDebuger.getInstance().getLogger().info("Размер главного лога: " + df.format(mainLogSize) + " MB.");

        File plSource = new File(CrazyDebuger.getInstance().getDataFolder() + "/players/latest/");
        plSource.getParentFile().mkdirs();

        double playerDirSize = getDirectorySizeMegaBytes(plSource);
        CrazyDebuger.getInstance().getLogger().info("Размер директории игроков: " + df.format(playerDirSize) + " MB.");

        //Если прошел месяц, то запускаем
        //Если размер директории или главного лога превышает 150 мб, то запускаем архивирование все равно.
        if (time > ZIP_TIME || mainLogSize > 150 || playerDirSize > 150) {

            try {
                runArchive(mainSource, plSource);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                this.saveTimer.setReady(true);
            }
            return;
        }

        this.saveTimer.setReady(true);
    }

    private void runArchive(File mainSource, File plSource) {
        CrazyDebuger.getInstance().getLogger().info("Начинаю архивирование всех логов.. ");

        long start = System.currentTimeMillis();

        this.lastZip = start;

        saveLastZip();

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat jdf = new SimpleDateFormat("HH-mm-ss   dd.MM.yyyy");
        String java_date = jdf.format(date);

        File plNew = new File(CrazyDebuger.getInstance().getDataFolder() + "/players/old/players-" + java_date + ".zip");
        plNew.getParentFile().mkdirs();

        File[] files = plSource.listFiles();

        if (files != null) {
            try {
                Zipping.zipDir(plSource, plNew, false);
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

        CrazyDebuger.getInstance().getLogger().info("Архивирование отдельных игроков завершено.");

        File mainNew = new File(CrazyDebuger.getInstance().getDataFolder() + "/general/old/general-" + java_date + ".zip");
        mainNew.getParentFile().mkdirs();

        if (mainSource.exists()) {
            Zipping.zipFile(mainSource, mainNew);

            mainSource.delete();
        }

        CrazyDebuger.getInstance().getLogger().info("Архивирование главного лога завершено.");
        long end = System.currentTimeMillis();

        CrazyDebuger.getInstance().getLogger().info("Полное архивирование логов успешно завершено за " + (end - start) + "мс.");
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

}
