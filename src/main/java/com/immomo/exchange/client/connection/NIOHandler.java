package com.immomo.exchange.client.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by mengjun on 16/4/11.
 */
public interface NIOHandler {
	public boolean connect(SelectionKey sk) throws Exception;
	public void write() throws Exception;
	public void read() throws Exception;
	public void close();
	public boolean isClosed();
}
