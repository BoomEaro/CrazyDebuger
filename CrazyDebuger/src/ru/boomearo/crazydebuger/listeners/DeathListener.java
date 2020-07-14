package ru.boomearo.crazydebuger.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import ru.boomearo.crazydebuger.CrazyDebuger;

public class DeathListener implements Listener {
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDeathEvent(PlayerDeathEvent e) {
		if (e.getEntity().hasMetadata("NPC")) {
			return;
		}
    	String d = "";
    	for (ItemStack is : e.getDrops()) {
    		d = d + is.getType() + (is.getDurability() > 0 ? ":" + is.getDurability() : "") + "(" + is.getAmount() + ") ";
    	}
    	Player killer = e.getEntity().getKiller();
		CrazyDebuger.sendLogMessage(e.getEntity(), "Погиб(" + d +")." + (killer != null ? " Убил: " + killer.getName() : ""), true);
	}
}
