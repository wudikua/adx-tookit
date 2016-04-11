package com.immomo.exchange.client.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by mengjun on 16/4/11.
 */
public interface NIOHandler {
	public boolean connect(SelectionKey sk) throws Exception;
	public void write(SelectionKey sk) throws Exception;
	public void read(SelectionKey sk) throws Exception;
}
