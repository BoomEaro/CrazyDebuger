package ru.boomearo.crazydebuger.runnable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import ru.boomearo.crazydebuger.CrazyDebuger;

public class SaveTimer extends AbstractTimer {

    private final Map<String, List<String>> log = new HashMap<String, List<String>>();
    private final List<String> mainLog = new ArrayList<String>();

    private final Object lock = new Object();

    public SaveTimer(CrazyDebuger plugin) {
        super("SaveTimer", TimeUnit.SECONDS, 5);
    }

    @Override
    public void task() {
        save();
    }

    public void save() {
        try {
            if (!CrazyDebuger.getInstance().isReady()) {
                return;
            }
            
            //Получаем полную копию того что мы должны записать на диск.
            //Для этого синхронизируемся, делаем полное копирование, затем удаляем.
            //После чего доступ на запись будет разблокирована что увеличит пропускную способность
            Map<String, List<String>> tmpLog;
            List<String> tmpMainLog;
            synchronized (this.lock) {
                tmpLog = new HashMap<String, List<String>>(this.log);
                this.log.clear();
                
                tmpMainLog = new ArrayList<String>(this.mainLog);
                this.mainLog.clear();
            }
            
            //Далее работаем с локальной копией. Все что в этой копии - должно быть записано и этого нет в общих коллекциях.
            
            if (!tmpLog.isEmpty()) {
                for (Entry<String, List<String>> entry : tmpLog.entrySet()) {
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
            }
            if (!tmpMainLog.isEmpty()) {
                File file = new File(CrazyDebuger.getInstance().getDataFolder() + "/general/latest.log");
                file.getParentFile().mkdirs();
                FileWriter writer;
                try {
                    writer = new FileWriter(file, true);
                    BufferedWriter bufferWriter = new BufferedWriter(writer);
                    for (String ms : tmpMainLog) {
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addLog(String name, String msg) {
        synchronized (this.lock) {
            if (name != null) {
                List<String> l = this.log.get(name);
                if (l == null) {
                    l = new ArrayList<String>();
                    this.log.put(name, l);
                }
                l.add(msg);
            }
            
            this.mainLog.add(msg);
        }
    }
}
