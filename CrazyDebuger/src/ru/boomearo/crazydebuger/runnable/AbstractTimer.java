package ru.boomearo.crazydebuger.runnable;

import java.util.concurrent.TimeUnit;

import ru.boomearo.crazydebuger.CrazyDebuger;

public abstract class AbstractTimer extends Thread {

    private volatile boolean cancel = false;
    
    private final long time;
    
    public AbstractTimer(String name, TimeUnit unit, long time) {
        super(name + "-Thread");
        this.time = unit.toMillis(time);
    }
    
    @Override
    public void run() {
        CrazyDebuger.getInstance().getLogger().info(this.getName() + " успешно запущен!");
        while (!this.cancel) {  
            try {
                Thread.sleep(this.time);
                
                task();
            } 
            catch (Throwable t) {
                CrazyDebuger.getInstance().getLogger().warning(this.getName() + " успешно был прерван!");
                return;
            }
        }
        CrazyDebuger.getInstance().getLogger().warning(this.getName() + " успешно завершен!");
    }
    
    public void cancelTask(boolean force) {
        this.cancel = true;
    }
    
    public abstract void task();
}
