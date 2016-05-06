package com.immomo.exchange.client.connection;

import com.google.common.collect.Maps;
import com.immomo.exchange.client.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by mengjun on 16/4/1.
 */
public class ConnectionPool {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

	private Map<String, LinkedBlockingDeque<Connection>> free;

	private BlockingQueue<Connection> closing = new ArrayBlockingQueue<Connection>(Config.maxConnections);

	private class ConnectionPoolCleaner implements Runnable {

		@Override
		public void run() {
			while (true) {
				Connection c = null;
				try {
					c = closing.poll(1, TimeUnit.SECONDS);
					if (c != null && !c.isClosed()) {
						c.close();
					}
				} catch (InterruptedException e) {
					logger.error("polling error", e);
				}
			}
		}
	}

	public ConnectionPool() {
		// clean thread
		new Thread(new ConnectionPoolCleaner()).start();
		this.free = Maps.newConcurrentMap();
	}

	public Connection get(String key) {
		if (free.get(key) == null) {
			synchronized (key.intern()) {
				if (free.get(key) == null) {
					free.put(key, new LinkedBlockingDeque<Connection>());
				}
			}
			return null;
		}
		try {
			LinkedBlockingDeque<Connection> conns = free.get(key);
			if (conns == null || conns.size() == 0) {
				return null;
			}
			Connection first = null;
			long now = System.currentTimeMillis();
			while ((first = conns.pollFirst()) != null) {
				if (first.getCreateTime() + Config.maxConnectionTime < now ||
						first.getLastActiveTime() + Config.connectionExpireTime < now) {
					// expire
					logger.info("expire a connection {}", first);
					closing.add(first);
				} else {
					return first;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void add(Connection conn) {
		if (!free.containsKey(conn.getCacheKey())) {
			free.put(conn.getCacheKey(), new LinkedBlockingDeque<Connection>());
		}
		logger.debug("return connection {}", conn.hashCode());
		conn.updateActiveTime();
		try {
			free.get(conn.getCacheKey()).putLast(conn);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static String getKey(String host, int port) {
		return host + port;
	}

}
