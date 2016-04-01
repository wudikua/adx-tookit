package com.immomo.exchange.client;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.util.LinkedList;

/**
 * Created by mengjun on 16/4/1.
 */
public class ConnectionPool {

	private Multimap<String, Connection> free;

	public ConnectionPool() {
		this.free = LinkedListMultimap.create();
	}

	public Connection get(String key) {
		if (free == null) {
			return null;
		}
		LinkedList<Connection> conns = (LinkedList<Connection>) free.get(key);
		if (conns == null) {
			return null;
		}
		Connection first = conns.getFirst();
		return first;
	}

	public void add(String key, Connection conn) {
		free.put(key, conn);
	}

	public static String getKey(String host, int port) {
		return host + port;
	}

}
