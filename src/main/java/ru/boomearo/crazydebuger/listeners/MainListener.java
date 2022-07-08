package ru.boomearo.crazydebuger.listeners;

import java.util.List;

import lombok.Value;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.BookMeta;

import ru.boomearo.crazydebuger.managers.LoggerManager;
import ru.boomearo.crazydebuger.objects.logger.LogLevel;
import ru.boomearo.crazydebuger.objects.logger.LogMessage;

@Value
public class MainListener implements Listener {

    LoggerManager loggerManager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent e) {
        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(e.getPlayer())
                .textAction((e.isCancelled() ? "(#): " : "") + "\"" + e.getMessage() + "\"")
                .logLevel(LogLevel.INFO)
                .build()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandEvent(PlayerCommandPreprocessEvent e) {
        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(e.getPlayer())
                .textAction((e.isCancelled() ? "(#): " : "") + "\"" + e.getMessage() + "\"")
                .logLevel(LogLevel.INFO)
                .build()
        );
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKickEvent(PlayerKickEvent e) {
        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(e.getPlayer())
                .textAction("Кикнут: " + e.getReason())
                .action(true)
                .logLevel(LogLevel.INFO)
                .build()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(e.getPlayer())
                .textAction("Подключился.")
                .action(true)
                .logLevel(LogLevel.INFO)
                .build()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(e.getPlayer())
                .textAction("Отключился.")
                .action(true)
                .logLevel(LogLevel.INFO)
                .build()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerEditBookEvent(PlayerEditBookEvent e) {
        if (e.isCancelled()) {
            return;
        }
        BookMeta bm = e.getNewBookMeta();
        if (bm != null) {
            List<String> pages = bm.getPages();
            if (pages != null) {
                String tile = bm.getTitle();
                StringBuilder ss = new StringBuilder((tile != null ? tile + ":" : ""));
                for (String msg : pages) {
                    ss.append("'").append(msg).append("' ");
                }

                this.loggerManager.sendLogMessage(LogMessage.builder()
                        .entity(e.getPlayer())
                        .textAction("Написал книгу: " + ss)
                        .action(true)
                        .logLevel(LogLevel.INFO)
                        .build()
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSignChangeEvent(SignChangeEvent e) {
        if (e.isCancelled()) {
            return;
        }
        this.loggerManager.sendLogMessage(LogMessage.builder()
                .entity(e.getPlayer())
                .textAction("Создал табличку: '" + e.getLine(0) + "' '" + e.getLine(1) + "' '" + e.getLine(2) + "' '" + e.getLine(3) + "'")
                .action(true)
                .logLevel(LogLevel.INFO)
                .build()
        );
    }

    //TODO я хз, нужно ли проверять наковальню, ибо там может быть срач из логов
	/*@EventHandler(priority = EventPriority.MONITOR)
	public void onPrepareAnvilEvent(PrepareAnvilEvent e) {
		if (e.getResult() == null) {
			return;
		}
		if (e.getView().getPlayer() instanceof Player) {
			Player pl = (Player) e.getView().getPlayer();
			ItemStack result = e.getResult();
			ItemMeta im = result.getItemMeta();
			if (im != null) {
				String dys = im.getDisplayName();
				if (dys != null) {
					CrazyDebuger.sendLogMessage(pl, "Назвал предмет " + result.getType() + ": " + dys, true);
				}
			}
		}
	}*/
}
