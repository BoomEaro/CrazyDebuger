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

import lombok.Getter;
import lombok.Setter;
import ru.boomearo.crazydebuger.CrazyDebuger;
import ru.boomearo.crazydebuger.objects.logger.LogEntry;

@Setter
@Getter
public class SaveTimer extends AbstractTimer {

    private final Map<String, List<LogEntry>> log = new HashMap<>();
    private final List<LogEntry> mainLog = new ArrayList<>();

    //По умолчанию логгер не готов
    private volatile boolean ready = false;

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
            //Приостанавливаем сохранение в лог если логгер не был готов по каким то причинам
            if (!this.ready) {
                return;
            }

            //Получаем полную копию того что мы должны записать на диск.
            //Для этого синхронизируемся, делаем полное копирование, затем удаляем.
            //После чего доступ на запись будет разблокирована что увеличит пропускную способность
            Map<String, List<LogEntry>> tmpLog;
            List<LogEntry> tmpMainLog;
            synchronized (this.lock) {
                tmpLog = new HashMap<>(this.log);
                this.log.clear();

                tmpMainLog = new ArrayList<>(this.mainLog);
                this.mainLog.clear();
            }

            //Далее работаем с локальной копией. Все что в этой копии - должно быть записано и этого нет в общих коллекциях.

            if (!tmpLog.isEmpty()) {
                File folder = CrazyDebuger.getInstance().getDataFolder();
                for (Entry<String, List<LogEntry>> entry : tmpLog.entrySet()) {
                    File file = new File(folder + "/players/latest/" + entry.getKey() + ".log");
                    file.getParentFile().mkdirs();

                    try (BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(file, true))) {
                        for (LogEntry ms : entry.getValue()) {
                            bufferWriter.write("[" + this.df.format(new Date(ms.getTime())) + "] [" + ms.getLogLevel().toString() + "] " + ms.getMessage() + "\n");
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!tmpMainLog.isEmpty()) {
                File folder = CrazyDebuger.getInstance().getDataFolder();
                File file = new File(folder + "/general/latest.log");
                file.getParentFile().mkdirs();

                try (BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(file, true))) {
                    for (LogEntry ms : tmpMainLog) {
                        bufferWriter.write("[" + this.df.format(new Date(ms.getTime())) + "] [" + ms.getLogLevel().toString() + "] " + ms.getMessage() + "\n");
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
                    l = new ArrayList<>();
                    this.log.put(name, l);
                }
                l.add(msg);
            }

            this.mainLog.add(msg);
        }
    }
}
