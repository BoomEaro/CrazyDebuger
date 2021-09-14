package ru.boomearo.crazydebuger.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import ru.boomearo.crazydebuger.CrazyDebuger;

public class DeathListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathEvent(PlayerDeathEvent e) {
        Player pl = e.getEntity();
        if (pl.hasMetadata("NPC")) {
            return;
        }
        StringBuilder d = new StringBuilder();
        for (ItemStack is : e.getDrops()) {
            d.append(CrazyDebuger.getNormalizedItemName(is)).append(" ");
        }
        Player killer = pl.getKiller();
        CrazyDebuger.sendLogMessage(pl, "Погиб(" + d + ")." + (killer != null ? " Убил: " + killer.getName() : ""), true);
    }
}
