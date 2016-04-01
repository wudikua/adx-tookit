package com.immomo.exchange.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Created by mengjun on 16/4/1.
 */
public class Connection {

	private SocketChannel conn;

	private String host;

	private int port;

	private boolean closed = false;

	public Connection(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void connect() throws IOException {
		conn.connect(new InetSocketAddress(host, port));
	}

	public void close() {
	}

}
