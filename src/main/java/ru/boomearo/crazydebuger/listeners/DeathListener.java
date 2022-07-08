package ru.boomearo.crazydebuger.listeners;

import lombok.Value;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import ru.boomearo.crazydebuger.managers.LoggerManager;
import ru.boomearo.crazydebuger.objects.logger.LogLevel;
import ru.boomearo.crazydebuger.objects.logger.LogMessage;

@Value
public class DeathListener implements Listener {

    LoggerManager loggerManager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathEvent(PlayerDeathEvent e) {
        if (!this.loggerManager.isDeathEnabled()) {
            return;
        }

        Player pl = e.getEntity();
        if (pl.hasMetadata("NPC")) {
            return;
        }
        StringBuilder d = new StringBuilder();
        for (ItemStack is : e.getDrops()) {
            d.append(LoggerManager.getNormalizedItemName(is)).append(" ");
        }
        Player killer = pl.getKiller();

        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(pl)
                .textAction("Погиб(" + d + ")." + (killer != null ? " Убил: " + killer.getName() : ""))
                .action(true)
                .logLevel(LogLevel.INFO)
                .build()
        );
    }
}
