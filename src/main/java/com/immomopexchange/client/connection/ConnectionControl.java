package com.immomo.exchange.client.connection;

import com.google.common.collect.Maps;
import com.immomo.exchange.client.Config;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mengjun on 16/5/3.
 */
public class ConnectionControl {

	private static Map<String, Semaphore> sem = Maps.newConcurrentMap();

	private static Lock initLock = new ReentrantLock();

	public static boolean tryAcquire(String key) {
		Semaphore s = sem.get(key);
		if (s == null) {
			initLock.lock();
			s = sem.get(key);
			if (s == null) {
				s = new Semaphore(Config.maxPerHostConnections);
				sem.put(key, s);
			}
			initLock.unlock();
		}
		return s.tryAcquire();
	}

	public static void release(String key) {
		Semaphore s = sem.get(key);
		if (s == null) {
			initLock.lock();
			s = sem.get(key);
			if (s == null) {
				s = new Semaphore(Config.maxPerHostConnections);
				sem.put(key, s);
			}
			initLock.unlock();
		}
		s.release();
	}

}
