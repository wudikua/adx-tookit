package com.immomo.exchange.client;

import com.immomo.exchange.client.connection.Connection;
import com.immomo.exchange.client.connection.ConnectionPool;
import com.immomo.exchange.client.protocal.Response;
import com.immomo.exchange.client.nio.MultiThreadSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.Future;

/**
 * Created by mengjun on 16/4/1.
 */
public class Client {

	private static final Logger logger = LoggerFactory.getLogger(Client.class);

	public static ConnectionPool pool = new ConnectionPool();

	private MultiThreadSelector selector;

	public Client(MultiThreadSelector selector) {
		this.selector = selector;
	}

	public Future<Response> get(String url) throws Exception {
		URL parsed = new URL(url);
		Connection conn = getConnection(parsed);
		conn.prepareConnect();
		return new ResponseFuture(conn);
	}

	private Connection getConnection(URL parsed) {
		String cacheKey = ConnectionPool.getKey(parsed.getHost(), parsed.getPort());
		Connection conn = pool.get(cacheKey);
		if (conn == null) {
			conn = new Connection(parsed, selector);
			logger.debug("new connection {}", conn.hashCode());
		} else {
			logger.debug("get connection {} from pool", conn.hashCode());
		}
		return conn;
	}

}
