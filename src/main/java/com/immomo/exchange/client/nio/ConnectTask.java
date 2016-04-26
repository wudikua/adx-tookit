package com.immomo.exchange.client.nio;

import com.immomo.exchange.client.connection.NIOHandler;

import java.nio.channels.SelectionKey;

/**
 * Created by mengjun on 16/4/26.
 */
public class ConnectTask extends NIOTask {

	public ConnectTask(NIOHandler handler, SelectionKey sk) {
		super(handler, sk);
	}

	@Override
	public void run() {
		try {
			handler.connect(sk);
		} catch (Exception e) {
			if (handler != null) {
				handler.close();
			}
			if (sk != null) {
				sk.cancel();
			}
			e.printStackTrace();
		}
	}
}
