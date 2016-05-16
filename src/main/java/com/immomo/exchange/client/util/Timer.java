package com.immomo.exchange.client.util;

import com.google.common.collect.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by wudikua on 2016/4/10.
 */
public class Timer {

	public static abstract class TimerUnit implements Comparable {
		public long deadline;
		public volatile boolean done = false;

		public TimerUnit(long timeout) {
			this.deadline = System.currentTimeMillis() + timeout;
		}

		public abstract void onTime() throws Exception;

		public int compareTo(Object o) {
			return this.hashCode() == o.hashCode() ? 0 : 1;
		}
	}

	private static TreeMap<Long, List<TimerUnit>> timers = new TreeMap<Long, List<TimerUnit>>();

	public static int MIN_TIMEOUT = 10;

	private static final Object mutex = new Object();

	static {
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					try {
						schedule();
						Thread.sleep(MIN_TIMEOUT);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}


	public static synchronized void addTimeout(TimerUnit timer) {
		List<TimerUnit> tList = timers.get(timer.deadline);
		if (tList == null) {
			synchronized (mutex) {
				tList = timers.get(timer.deadline);
				if (tList == null) {
					tList = new LinkedList<TimerUnit>();
					timers.put(timer.deadline, tList);
				}
			}
		}
		tList.add(timer);
	}

	private static synchronized void schedule() {
		long now = System.currentTimeMillis();
		while (!timers.keySet().isEmpty() && timers.firstKey() <= now) {
			Iterator<TimerUnit> it = timers.pollFirstEntry().getValue().iterator();
			while (it.hasNext()) {
				TimerUnit timer = it.next();
				if (timer.done) {
					continue;
				}
				try {
					timer.onTime();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
