package ru.boomearo.crazydebuger.listeners;

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
import ru.boomearo.crazydebuger.objects.logger.LogEntry;
import ru.boomearo.crazydebuger.objects.logger.LogLevel;

public class ItemListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDespawnEvent(ItemDespawnEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Item en = e.getEntity();
        ItemStack item = en.getItemStack();

        Location loc = en.getLocation();

        //String ip, String money, double x, double y, double z, String world, String entity, String info, boolean isAction
        CrazyDebuger.getInstance().getSaveTimer().addLog(null, new LogEntry(System.currentTimeMillis(), LogLevel.WARNING, CrazyDebuger.craftMsgLog(null, null, loc.getX(), loc.getY(), loc.getZ(), en.getWorld().getName(), CrazyDebuger.getNormalizedItemName(item), "Деспавн.", true)));

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPickupItemEvent(EntityPickupItemEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Item en = e.getItem();
        ItemStack item = en.getItemStack();

        Location loc = en.getLocation();

        Entity ee = e.getEntity();

        CrazyDebuger.getInstance().getSaveTimer().addLog(null, new LogEntry(System.currentTimeMillis(), LogLevel.WARNING, CrazyDebuger.craftMsgLog(null, null, loc.getX(), loc.getY(), loc.getZ(), en.getWorld().getName(), CrazyDebuger.getNormalizedItemName(item), "Подобран сущностью " + ee.getType().name() + " (" + ee.getName() + ")", true)));

    }

}
