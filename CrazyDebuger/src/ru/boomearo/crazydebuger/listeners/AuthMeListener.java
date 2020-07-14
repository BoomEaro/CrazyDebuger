package ru.boomearo.crazydebuger.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import fr.xephi.authme.events.EmailChangedEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.events.RegisterEvent;
import fr.xephi.authme.events.UnregisterByPlayerEvent;
import ru.boomearo.crazydebuger.CrazyDebuger;

public class AuthMeListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
    public void onLoginEvent(LoginEvent e) {
		CrazyDebuger.sendLogMessage(e.getPlayer(), "Успешно авторизировался.", true);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onLogoutEvent(LogoutEvent e) {
		CrazyDebuger.sendLogMessage(e.getPlayer(), "Успешно вышел из системы.", true);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onUnregisterByPlayerEvent(UnregisterByPlayerEvent e) {
		CrazyDebuger.sendLogMessage(e.getPlayer(), "Успешно удалил запись регистрации.", true);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onRegisterEvent(RegisterEvent e) {
		CrazyDebuger.sendLogMessage(e.getPlayer(), "Успешно зарегистрирован.", true);
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEmailChangedEvent(EmailChangedEvent e) {
		CrazyDebuger.sendLogMessage(e.getPlayer(), "Изменил парамерты почты.", true);
	}
}
