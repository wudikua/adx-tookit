package com.immomo.exchange.client.event;

import com.immomo.exchange.client.connection.Connection;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by mengjun on 16/4/8.
 */
public interface NIOEvent {
	public NIOEventType getType();
	public SocketChannel getChannel();
	public Connection getConnection();
	public int getOp();
	public SelectionKey getSelectionKey();
}
