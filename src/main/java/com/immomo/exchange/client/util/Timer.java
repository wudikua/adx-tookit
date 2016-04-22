package com.immomo.exchange.client.util;

import com.google.common.collect.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by wudikua on 2016/4/10.
 */
public class Timer {

	private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static final long tickUnit = Long.parseLong(System.getProperty("notify.systimer.tick", "20"));
	private static volatile long time = System.currentTimeMillis();

	private static class TimerTicker implements Runnable {
		public void run() {
			time = System.currentTimeMillis();
		}
	}

	public static long currentTimeMillis() {
		return time;
	}

	static {
		executor.scheduleAtFixedRate(new TimerTicker(), tickUnit, tickUnit, TimeUnit.MILLISECONDS); Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				executor.shutdown();
			}
		});
	}

	public static abstract class TimerUnit implements Comparable {
		public long deadline;

		public TimerUnit(long timeout) {
			this.deadline = Timer.currentTimeMillis() + timeout;
		}

		public abstract void onTime() throws Exception;

		public int compareTo(Object o) {
			return this.hashCode() == o.hashCode() ? 0 : 1;
		}
	}

	private static TreeMultimap<Long, TimerUnit> timers = TreeMultimap.create(Ordering.natural().reverse(), Ordering.<TimerUnit>natural());

	public static int MIN_TIMEOUT = 100;

	static {
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					schedule();
					try {
						Thread.sleep(MIN_TIMEOUT);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	public static void addTimeout(TimerUnit timer) {
		timers.put(timer.deadline, timer);
	}

	private static void schedule() {
		long now = Timer.currentTimeMillis();
		long deadline = 0;
		while (timers.keySet().size() > 0 && (deadline = timers.keySet().first()) <= now) {
			for (TimerUnit timer : timers.get(deadline)) {
				try {
					timer.onTime();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			timers.removeAll(deadline);
		}
	}

}
