package ru.boomearo.crazydebuger.runnable;

import java.util.concurrent.TimeUnit;

import ru.boomearo.crazydebuger.CrazyDebuger;

public class ZipRunner extends AbstractTimer {
	
	public ZipRunner() {
		super("ZipRunner", TimeUnit.HOURS, 12);
	}
	
	@Override
	public void task() {
		try {
		    CrazyDebuger.getInstance().checkOutdateFiles();
		}
		catch (Exception e) {
		    e.printStackTrace();
		}
	}

}
