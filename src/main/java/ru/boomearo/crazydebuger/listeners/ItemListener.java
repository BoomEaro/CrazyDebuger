package ru.boomearo.crazydebuger.listeners;

import lombok.Value;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;

import ru.boomearo.crazydebuger.managers.LoggerManager;
import ru.boomearo.crazydebuger.objects.logger.LogLevel;
import ru.boomearo.crazydebuger.objects.logger.LogMessage;

@Value
public class ItemListener implements Listener {

    LoggerManager loggerManager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDespawnEvent(ItemDespawnEvent e) {
        if (e.isCancelled()) {
            return;
        }

        if (!this.loggerManager.isItemEnabled()) {
            return;
        }

        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(e.getEntity())
                .textAction("Деспавн")
                .logLevel(LogLevel.WARNING)
                .action(true)
                .build()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPickupItemEvent(EntityPickupItemEvent e) {
        if (e.isCancelled()) {
            return;
        }

        if (!this.loggerManager.isItemEnabled()) {
            return;
        }

        Entity ee = e.getEntity();

        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(ee)
                .textAction("Подобран сущностью " + ee.getType().name() + " (" + ee.getName() + ")")
                .logLevel(LogLevel.WARNING)
                .action(true)
                .build()
        );
    }
}
