package com.immomo.exchange.client;

import com.google.common.collect.Lists;
import com.immomo.exchange.client.connection.Connection;
import com.immomo.exchange.client.connection.ConnectionPool;
import com.immomo.exchange.client.nio.SingleThreadSelector;
import com.immomo.exchange.client.protocal.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

/**
 * Created by mengjun on 16/4/1.
 */
public class Client {

	private static final Logger logger = LoggerFactory.getLogger(Client.class);

	public static ConnectionPool pool = new ConnectionPool();

	private List<SingleThreadSelector> selector;

	private int ioThreads;

	private Random r = new Random();

	public Client(SingleThreadSelector selector) {
		this.selector = Lists.newArrayList(selector);
		ioThreads = 1;
	}

	public Client(List<SingleThreadSelector> selector) {
		this.selector = selector;
		ioThreads = selector.size();
	}

	public Future<Response> get(String url) throws Exception {
		URL parsed = new URL(url);
		Connection conn = getConnection(parsed);
		conn.prepareConnect(parsed);
		ResponseFuture future = new ResponseFuture(conn);
		conn.setFuture(future);
		return future;
	}

	private Connection getConnection(URL parsed) {
		String cacheKey = ConnectionPool.getKey(parsed.getHost(), parsed.getPort());
		while(true) {
			Connection conn = pool.get(cacheKey);
			if (conn == null) {
				int ioThread = r.nextInt(ioThreads);
				conn = new Connection(selector.get(ioThread));
				logger.debug("{} new connection {}", Thread.currentThread().getName(), conn.hashCode());
				return conn;
			} else {
				logger.debug("{} get connection {} from pool", Thread.currentThread().getName(), conn.hashCode());
			}
			return conn;
		}
	}

}
