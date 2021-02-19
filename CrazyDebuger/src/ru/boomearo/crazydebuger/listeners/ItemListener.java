package ru.boomearo.crazydebuger.listeners;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;

import ru.boomearo.crazydebuger.CrazyDebuger;

public class ItemListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDespawnEvent(ItemDespawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        
        Item en = e.getEntity();
        ItemStack item = en.getItemStack();
        
        Location loc = en.getLocation();
        
        Date date = new Date(System.currentTimeMillis()); 
        SimpleDateFormat jdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        String java_date = jdf.format(date);

        String msg = CrazyDebuger.craftMainMsg(java_date, null, null, loc.getX(), loc.getY(), loc.getZ(), en.getWorld().getName(), CrazyDebuger.getNormalizedItemName(item));
        
        CrazyDebuger.getInstance().getSaveTimer().addLog(null, msg.replace("\n", " ") + "Деспавн.\n");
        
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPickupItemEvent(EntityPickupItemEvent e) {
        if (e.isCancelled()) {
            return;
        }
        
        Item en = e.getItem();
        ItemStack item = en.getItemStack();
        
        Location loc = en.getLocation();
        
        Date date = new Date(System.currentTimeMillis()); 
        SimpleDateFormat jdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        String java_date = jdf.format(date);

        String msg = CrazyDebuger.craftMainMsg(java_date, null, null, loc.getX(), loc.getY(), loc.getZ(), en.getWorld().getName(), CrazyDebuger.getNormalizedItemName(item));
        
        Entity ee = e.getEntity();
        
        CrazyDebuger.getInstance().getSaveTimer().addLog(null, msg.replace("\n", " ") + "Подобран сущностью " + ee.getType().name() + " (" + ee.getName()  + ")" + "\n");
    }
    
}
