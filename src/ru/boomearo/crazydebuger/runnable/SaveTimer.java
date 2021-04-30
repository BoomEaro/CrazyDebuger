package ru.boomearo.crazydebuger.runnable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import ru.boomearo.crazydebuger.CrazyDebuger;
import ru.boomearo.crazydebuger.objects.logger.LogEntry;

public class SaveTimer extends AbstractTimer {

    private final Map<String, List<LogEntry>> log = new HashMap<String, List<LogEntry>>();
    private final List<LogEntry> mainLog = new ArrayList<LogEntry>();

    private final Object lock = new Object();
    
    private final DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");

    public SaveTimer() {
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
            Map<String, List<LogEntry>> tmpLog;
            List<LogEntry> tmpMainLog;
            synchronized (this.lock) {
                tmpLog = new HashMap<String, List<LogEntry>>(this.log);
                this.log.clear();
                
                tmpMainLog = new ArrayList<LogEntry>(this.mainLog);
                this.mainLog.clear();
            }
            
            //Далее работаем с локальной копией. Все что в этой копии - должно быть записано и этого нет в общих коллекциях.
            
            if (!tmpLog.isEmpty()) {
                for (Entry<String, List<LogEntry>> entry : tmpLog.entrySet()) {
                    File file = new File(CrazyDebuger.getInstance().getDataFolder() + "/players/latest/" + entry.getKey() + ".log");
                    file.getParentFile().mkdirs();
                    
                    try (BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(file, true))) {
                        for (LogEntry ms : entry.getValue()) {
                            bufferWriter.write("[" + df.format(new Date(ms.getTime())) + "] [" + ms.getLogLevel().toString() + "] " + ms.getMessage() + "\n");
                        }
                    } 
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!tmpMainLog.isEmpty()) {
                File file = new File(CrazyDebuger.getInstance().getDataFolder() + "/general/latest.log");
                file.getParentFile().mkdirs();
                
                try (BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(file, true))) {
                    for (LogEntry ms : tmpMainLog) {
                        bufferWriter.write("[" + df.format(new Date(ms.getTime())) + "] [" + ms.getLogLevel().toString() + "] " + ms.getMessage() + "\n");
                    }
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addLog(String name, LogEntry msg) {
        synchronized (this.lock) {
            if (name != null) {
                List<LogEntry> l = this.log.get(name);
                if (l == null) {
                    l = new ArrayList<LogEntry>();
                    this.log.put(name, l);
                }
                l.add(msg);
            }
            
            this.mainLog.add(msg);
        }
    }
}
