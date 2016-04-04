package com.immomo.exchange.client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by wudikua on 2016/4/4.
 */
public class MultiThreadSelector implements Runnable {

	public static Selector selector;

	private int count;

	private boolean started = false;

	public MultiThreadSelector(int count) {
		this.count = count;
	}

	public Selector getSelector() {
		return selector;
	}

	public void init() throws IOException {
		if (started) {
			return;
		}
		selector = Selector.open();
	}

	public void start() {
		for (int i=0; i<count; i++) {
			new Thread(new MultiThreadSelector(count)).start();
		}
	}

	public void run() {
		while(true) {
			try {
				int select = selector.select();
				if (select > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();
					Iterator<SelectionKey> it = keys.iterator();
					while (it.hasNext()) {
						SelectionKey sk = it.next();
						Connection conn = (Connection) sk.attachment();
						if (sk.isWritable()) {
							conn.write();
						} else if (sk.isReadable()) {
							conn.read();
						}
						it.remove();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}
