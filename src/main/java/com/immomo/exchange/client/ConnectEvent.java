package com.immomo.exchange.client;

import java.nio.channels.SocketChannel;

/**
 * Created by mengjun on 16/4/8.
 */
public class ConnectEvent implements NIOEvent {
	private SocketChannel channel;
	private Connection connection;
	private int op;

	public ConnectEvent(SocketChannel channel, Connection connection, int op) {
		this.channel = channel;
		this.connection = connection;
		this.op = op;
	}

	public SocketChannel getChannel() {
		return channel;
	}

	public Connection getConnection() {
		return null;
	}

	public int getOp() {
		return 0;
	}
}
