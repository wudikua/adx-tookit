package com.immomo.exchange.client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wudikua on 2016/4/4.
 */
public class MultiThreadSelector implements Runnable {

	private static Selector selector;

	private Queue<NIOEvent> pending = new LinkedBlockingDeque<NIOEvent>();

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

	public void register(SocketChannel channel, int op, Connection connection) {
		pending.add(new ConnectEvent(channel, connection, op));
		selector.wakeup();
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
						if (sk.isConnectable()) {
							conn.connect();
						} else if (sk.isWritable()) {
							conn.write();
						} else if (sk.isReadable()) {
							conn.read();
						}
						it.remove();
					}
				}
				if (pending.size() > 0) {
					Iterator<NIOEvent> it = pending.iterator();
					while(it.hasNext()) {
						NIOEvent e = it.next();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
