package com.immomo.exchange.client;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mengjun on 16/4/1.
 */
public class ConnectionPool {

	private Multimap<String, Connection> free;

	public ConnectionPool() {
		this.free = LinkedListMultimap.create();
	}

	public Connection get(String key) {
		synchronized (key.intern()) {
			if (free == null) {
				return null;
			}
			try {
				LinkedList<Connection> conns = (LinkedList<Connection>) free.get(key);
				if (conns == null || conns.size() == 0) {
					return null;
				}
				Connection first = conns.peekFirst();
				return first;
			} catch (Exception e) {
				return null;
			}
		}
	}

	public void add(String key, Connection conn) {
		free.put(key, conn);
	}

	public static String getKey(String host, int port) {
		return host + port;
	}

}
