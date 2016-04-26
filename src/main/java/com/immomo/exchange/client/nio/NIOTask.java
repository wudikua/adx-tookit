package com.immomo.exchange.client.nio;

import com.immomo.exchange.client.connection.NIOHandler;

import java.nio.channels.SelectionKey;

/**
 * Created by mengjun on 16/4/26.
 */
public abstract class NIOTask implements Runnable {

	protected NIOHandler handler;

	protected SelectionKey sk;

	public NIOTask(NIOHandler handler, SelectionKey sk) {
		this.handler = handler;
		this.sk = sk;
	}
}
