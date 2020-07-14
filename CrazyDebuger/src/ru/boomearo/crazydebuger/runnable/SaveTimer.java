package ru.boomearo.crazydebuger.runnable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.scheduler.BukkitRunnable;

import ru.boomearo.crazydebuger.CrazyDebuger;

public class SaveTimer extends BukkitRunnable {
	
	private static volatile boolean isSaving = false;
	
	private static volatile Map<String, List<String>> log = new HashMap<String, List<String>>();
	private static volatile List<String> mainLog = new ArrayList<String>();
	
	public SaveTimer(CrazyDebuger plugin) {
		runnable();
	}
	
	public void runnable() {
		this.runTaskTimerAsynchronously(CrazyDebuger.getInstance(), 20*5, 20*5);
	}
	
	@Override
	public void run() {
		save();
	}
	
    public static void save() {
    	if (!CrazyDebuger.getInstance().isReady()) {
    		return;
    	}
		if (isSaving) {
			return;
		}
		isSaving = true;
		try {
			synchronized (log) {
		    	if (!log.isEmpty()) {
		    		for (Entry<String, List<String>> entry : log.entrySet()) {
		    			File file = new File(CrazyDebuger.getInstance().getDataFolder() + "/players/latest/" + entry.getKey() + ".log");
		    			file.getParentFile().mkdirs();
		    			FileWriter writer;
		    			try {
		    				writer = new FileWriter(file, true);
		    				BufferedWriter bufferWriter = new BufferedWriter(writer);
							
		    				for (String ms : entry.getValue()) {
		    					try {
		        					bufferWriter.write(ms);
		    					}
		    					catch (Exception e) {
		    		   				e.printStackTrace();
		    					}
		    				}
		    				bufferWriter.close();
		    			} 
		    			catch (Exception e) {
		    				e.printStackTrace();
		    			}
		    		}
		    		log.clear();
		    	}
			}
			synchronized (mainLog) {
				if (!mainLog.isEmpty()) {
					File file = new File(CrazyDebuger.getInstance().getDataFolder() + "/general/latest.log");
					file.getParentFile().mkdirs();
					FileWriter writer;
					try {
						writer = new FileWriter(file, true);
						BufferedWriter bufferWriter = new BufferedWriter(writer);
						for (String ms : mainLog) {
							try {
								bufferWriter.write(ms);
							}
							catch (Exception e) {
				   				e.printStackTrace();
							}
						}
						bufferWriter.close();
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
					mainLog.clear();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			isSaving = false;
		}
    }
	
	public static boolean isSaveProgress() {
		return isSaving;
	}
	
	public synchronized static void addPlayerLog(String name, String msg) {
		List<String> l = log.get(name);
		if (l == null) {
			l = new ArrayList<String>();
			log.put(name, l);
		}
		l.add(msg);
	}
	
	public synchronized static void addMainLog(String msg) {
		mainLog.add(msg);
	}
}
