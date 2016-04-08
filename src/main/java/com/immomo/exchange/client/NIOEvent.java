package com.immomo.exchange.client;

import java.nio.channels.SocketChannel;

/**
 * Created by mengjun on 16/4/8.
 */
public interface NIOEvent {
	public SocketChannel getChannel();
	public Connection getConnection();
	public int getOp();
}
