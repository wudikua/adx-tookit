package com.immomo.exchange.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.channels.Selector;

/**
 * Created by mengjun on 16/4/1.
 */
public class Client {

	private ConnectionPool pool = new ConnectionPool();

	private MultiThreadSelector selector;

	public Client(MultiThreadSelector selector) {
		this.selector = selector;
	}

	public void get(String url) throws Exception {
		URL parsed = new URL(url);
		Connection conn = getConnection(parsed);
		conn.prepareConnect();
	}

	private Connection getConnection(URL parsed) {
		String cacheKey = ConnectionPool.getKey(parsed.getHost(), parsed.getPort());
		Connection conn = pool.get(cacheKey);
		if (conn == null) {
			conn = new Connection(parsed, selector);
		}
		return conn;
	}

}
