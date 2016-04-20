package com.immomo.exchange.client.event;

import com.immomo.exchange.client.connection.Connection;

import java.nio.channels.SocketChannel;

/**
 * Created by mengjun on 16/4/8.
 */
public class ChangeEvent implements NIOEvent {
	private SocketChannel channel;
	private Connection connection;
	private int op;

	public ChangeEvent(SocketChannel channel, Connection connection, int op) {
		this.channel = channel;
		this.connection = connection;
		this.op = op;
	}

	public NIOEventType getType() {
		return NIOEventType.CHANGE;
	}

	public SocketChannel getChannel() {
		return channel;
	}

	public Connection getConnection() {
		return connection;
	}

	public int getOp() {
		return op;
	}
}
