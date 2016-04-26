package com.immomo.exchange.client.connection;

import com.google.common.collect.Maps;
import com.immomo.exchange.client.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * Created by mengjun on 16/4/1.
 */
public class ConnectionPool {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

	private Map<String, LinkedList<Connection>> free;

	private BlockingQueue<Connection> closing = new ArrayBlockingQueue<Connection>(Config.maxConnections);

	private class ConnectionPoolCleaner implements Runnable {

		@Override
		public void run() {
			while (true) {
				Connection c = closing.poll();
				c.close();
			}
		}
	}

	public ConnectionPool() {
		// 清理线程
		new Thread(new ConnectionPoolCleaner()).start();
		this.free = Maps.newConcurrentMap();
	}

	public Connection get(String key) {
		synchronized (key.intern()) {
			if (free.get(key) == null) {
				free.put(key, new LinkedList<Connection>());
				return null;
			}
			try {
				LinkedList<Connection> conns = free.get(key);
				if (conns == null || conns.size() == 0) {
					return null;
				}
				Connection first;
				long now = System.currentTimeMillis();
				while ((first = conns.removeFirst()) != null) {
					if (first.getCreateTime() + Config.maxConnectionTime > now ||
							first.getLastActiveTime() + Config.connectionExpireTime < now) {
						// expire
						closing.add(first);
					}
				}
				return first;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public void add(Connection conn) {
		synchronized (conn.getCacheKey().intern()) {
			if (!free.containsKey(conn.getCacheKey())) {
				free.put(conn.getCacheKey(), new LinkedList<Connection>());
			}
			logger.debug("return connection {}", conn.hashCode());
			conn.updateActiveTime();
	 		free.get(conn.getCacheKey()).addLast(conn);
		}
	}

	public static String getKey(String host, int port) {
		return host + port;
	}

}
