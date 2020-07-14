package ru.boomearo.crazydebuger.runnable;

import org.bukkit.scheduler.BukkitRunnable;

import ru.boomearo.crazydebuger.CrazyDebuger;

public class ZipRunner extends BukkitRunnable {

	private CrazyDebuger plugin;
	
	public ZipRunner(CrazyDebuger plugin) {
		this.plugin = plugin;
		runnable();
	}
	
	public void runnable() {
		//Каждые 12 часов делаем чекалку
		this.runTaskTimerAsynchronously(CrazyDebuger.getInstance(), 20*43200, 20*43200);
	}
	
	@Override
	public void run() {
		this.plugin.checkOutdateFiles();
	}

}
