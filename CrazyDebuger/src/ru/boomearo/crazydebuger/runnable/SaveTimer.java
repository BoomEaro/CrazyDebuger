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

    private Map<String, List<String>> log = new HashMap<String, List<String>>();
    private List<String> mainLog = new ArrayList<String>();

    private final Object lock = new Object();

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

    public void save() {
        if (!CrazyDebuger.getInstance().isReady()) {
            return;
        }
        try {
            synchronized (this.lock) {
                if (!this.log.isEmpty()) {
                    for (Entry<String, List<String>> entry : this.log.entrySet()) {
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
                    this.log.clear();
                }
                if (!this.mainLog.isEmpty()) {
                    File file = new File(CrazyDebuger.getInstance().getDataFolder() + "/general/latest.log");
                    file.getParentFile().mkdirs();
                    FileWriter writer;
                    try {
                        writer = new FileWriter(file, true);
                        BufferedWriter bufferWriter = new BufferedWriter(writer);
                        for (String ms : this.mainLog) {
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
                    this.mainLog.clear();
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addLog(String name, String msg) {
        synchronized (this.lock) {
            List<String> l = this.log.get(name);
            if (l == null) {
                l = new ArrayList<String>();
                this.log.put(name, l);
            }
            l.add(msg);
            
            this.mainLog.add(msg);
        }
    }
}
