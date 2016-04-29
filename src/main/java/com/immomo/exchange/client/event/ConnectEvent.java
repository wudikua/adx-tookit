package com.immomo.exchange.client.event;

import com.immomo.exchange.client.connection.Connection;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by mengjun on 16/4/8.
 */
public class ConnectEvent implements NIOEvent {
	private SocketChannel channel;
	private Connection connection;
	private SelectionKey sk;
	private int op;

	public ConnectEvent(SocketChannel channel, Connection connection, int op, SelectionKey sk) {
		this.channel = channel;
		this.connection = connection;
		this.op = op;
		this.sk = sk;
	}

	public NIOEventType getType() {
		return NIOEventType.CONNECT;
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

	@Override
	public SelectionKey getSelectionKey() {
		return sk;
	}
}
