package com.immomo.exchange.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
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

	private static final Logger logger = LoggerFactory.getLogger(MultiThreadSelector.class);

	private static Selector selector;

	private static Queue<NIOEvent> pending = new LinkedBlockingDeque<NIOEvent>();

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
		started = true;
		selector = Selector.open();
	}

	public void start() {
		for (int i=0; i<count; i++) {
			new Thread(new MultiThreadSelector(count)).start();
		}
	}

	public void register(SocketChannel channel, int op, Connection connection) {
		pending.add(new ConnectEvent(channel, connection, op));
		logger.debug("add pending queue {}", pending.size());
		selector.wakeup();
	}

	private void changeEvent() throws ClosedChannelException {
		logger.debug("check pending size is {}", pending.size());
		if (pending.size() > 0) {
			logger.debug("something is to be register");
			Iterator<NIOEvent> it = pending.iterator();
			while(it.hasNext()) {
				NIOEvent e = it.next();
				logger.debug("register {}", e.getConnection());
				e.getChannel().register(selector, e.getOp(), e.getConnection());
				pending.remove(e);
			}
		}
	}

	public void run() {
		while(true) {
			try {
				changeEvent();
				int select = selector.select();
				logger.debug("select is {}", select);
				if (select > 0) {
					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					while (it.hasNext()) {
						SelectionKey sk = it.next();
						it.remove();
						logger.debug("key op {}", sk.readyOps());
						Connection conn = (Connection) sk.attachment();
						if (sk.isConnectable()) {
							long begin = System.currentTimeMillis();
							conn.connect(sk);
							System.out.println("connect " +  (System.currentTimeMillis() - begin));
						} else if (sk.isWritable()) {
							long begin = System.currentTimeMillis();
							conn.write(sk);
							System.out.println("write " +  (System.currentTimeMillis() - begin));
						} else if (sk.isReadable()) {
							long begin = System.currentTimeMillis();
							conn.read(sk);
							System.out.println("read " +  (System.currentTimeMillis() - begin));
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
