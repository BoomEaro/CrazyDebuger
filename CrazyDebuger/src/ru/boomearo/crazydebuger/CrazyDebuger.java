package ru.boomearo.crazydebuger;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

import ru.boomearo.crazydebuger.listeners.AuthMeListener;
import ru.boomearo.crazydebuger.listeners.DeathListener;
import ru.boomearo.crazydebuger.listeners.MainListener;
import ru.boomearo.crazydebuger.runnable.SaveTimer;
import ru.boomearo.crazydebuger.runnable.ZipRunner;
import ru.boomearo.crazydebuger.utils.Ziping;

public class CrazyDebuger extends JavaPlugin {
	private IMoney money = null;
	
	private SaveTimer timer = null;
	private ZipRunner ziper = null;
	
	private boolean moneyEnabled = true;
	private boolean deathEnabled = true;
	private boolean authmeEnabled = true;
	private boolean runArchiveCheckTimer = true;
	
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
		}
		if (this.runArchiveCheckTimer) {
			if (this.ziper == null) {
				this.ziper = new ZipRunner(this);
			}
		}
		
	    getServer().getPluginManager().registerEvents(new MainListener(), this);
	    
	    if (this.deathEnabled) {
		    getServer().getPluginManager().registerEvents(new DeathListener(), this);
	    }
	    if ((Bukkit.getPluginManager().getPlugin("AuthMe") != null) && this.authmeEnabled) {
	    	getServer().getPluginManager().registerEvents(new AuthMeListener(), this);
	    }
	    
		getLogger().info("Плагин успешно включен.");
		
	}
	public void onDisable() {
		while (SaveTimer.isSaveProgress());
		SaveTimer.save();
		
		getLogger().info("Плагин успешно выключен.");
	}
	
	private static CrazyDebuger instance = null;
	public static CrazyDebuger getInstance() { 
		if (instance != null) return instance; return null; 
	}
	
	public void loadConfig() {
		this.moneyEnabled = getConfig().getBoolean("Configuration.moneyEnabled");
		this.deathEnabled = getConfig().getBoolean("Configuration.deathEnabled");
		this.authmeEnabled = getConfig().getBoolean("Configuration.authmeEnabled");
		this.runArchiveCheckTimer = getConfig().getBoolean("Configuration.runArchiveCheckTimer");
		
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
		return money.getMoney(name);
	}
	
	public boolean isMoneyEnabled() {
		return this.moneyEnabled;
	}
	public boolean isDeathEnabled() {
		return this.deathEnabled;
	}
	public boolean isAuthMeEnabled() {
		return this.authmeEnabled;
	}
	
    
	public static String craftMainMsg(String java_date, String ip, String moneys, int x, int y, int z, String world, String pName) {
		return "[" + java_date + "][{" + ip + "}( " + moneys + " (" + x + "|" + y + "|" + z + "|" + world + ")" + ") " + pName + "]: ";
	}
	
	public static void sendLogMessage(Player player, String info, boolean isAction) {
		CrazyDebuger cd = CrazyDebuger.getInstance();
		if (!cd.isReady()) {
			cd.getLogger().severe("Предотвращена попытка отправки в лог сообщения. Плагин не готов!");
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(cd, () -> {
			String pName = player.getName();
			Location loc = player.getLocation();
			int x = loc.getBlockX();
			int y = loc.getBlockY();
			int z = loc.getBlockZ();
			String world = loc.getWorld().getName();
			Date date = new Date(System.currentTimeMillis()); 
			SimpleDateFormat jdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
			String java_date = jdf.format(date);
			
			String msg = craftMainMsg(java_date, player.getAddress().getHostString(), CrazyDebuger.getInstance().getMoney(player.getName()), 
					x, y, z, world, pName) + (isAction ? "== " + ChatColor.stripColor(info.replace("\n", " ")) + " ==\n" : ChatColor.stripColor(info.replace("\n", " ")) + "\n");
			
			send(pName, msg);
			
		});
	}
	
	private static void send(String player, String msg) {
		//Поток добавляющий запись в коллкцию должен подождать неопределнное время если идет сохранение.
		while (SaveTimer.isSaveProgress());
		
		SaveTimer.addPlayerLog(player, msg);
		SaveTimer.addMainLog(msg);
		
	}
	
	//ess support
	private class GettingMoneyEmpty implements IMoney {
		@Override
		public String getMoney(String user) {
			return "";
		}

	}
	ggggggg
	//TODO исправить синхранизацию в SaveTimer
	public void checkOutdateFiles() { 
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
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
		});
	}
	
	public void checkOldDir() {
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
	
	private class GettingMoneyLegal implements IMoney {
		private Essentials ess;
		public GettingMoneyLegal(Plugin pl) {
			this.ess = (Essentials) pl;
		}
		@Override
		public String getMoney(String user) {
			BigDecimal bd = ess.getUser(user).getMoney();
			return NumberUtil.displayCurrency(bd, ess);
		}

	}
	
	private interface IMoney {

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
