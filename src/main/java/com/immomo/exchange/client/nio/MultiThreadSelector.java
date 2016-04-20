package com.immomo.exchange.client.nio;

import com.immomo.exchange.client.connection.Connection;
import com.immomo.exchange.client.connection.NIOHandler;
import com.immomo.exchange.client.event.ChangeEvent;
import com.immomo.exchange.client.event.ConnectEvent;
import com.immomo.exchange.client.event.NIOEvent;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.*;

/**
 * Created by wudikua on 2016/4/4.
 */
public class MultiThreadSelector implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(MultiThreadSelector.class);

	private static Selector selector;

	private static Queue<NIOEvent> pending = new ConcurrentLinkedQueue<NIOEvent>();

	private ExecutorService reactor = Executors.newFixedThreadPool(10);

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
		if (SelectionKey.OP_CONNECT == op) {
			pending.add(new ConnectEvent(channel, connection, op));
		} else {
			pending.add(new ChangeEvent(channel, connection, op));
		}
		logger.debug("add pending queue {}", pending.size());
		selector.wakeup();
	}

	private void changeEvent() {
		logger.debug("check pending size is {}", pending.size());
		if (pending.size() > 0) {
			logger.debug("something is to be register");
			Iterator<NIOEvent> it = pending.iterator();
			while(it.hasNext()) {
				NIOEvent e = it.next();
				it.remove();
				logger.debug("changEvent {}", e.getConnection());
				try {
					e.getChannel().register(selector, e.getOp(), e.getConnection());
				} catch (ClosedChannelException  ex) {
					ex.printStackTrace();
					e.getConnection().close();
				}
			}
		}
	}

	private abstract class NIOTask implements Runnable {

		protected NIOHandler handler;

		protected SelectionKey sk;

		private NIOTask(NIOHandler handler, SelectionKey sk) {
			this.handler = handler;
			this.sk = sk;
		}
	}

	private class ConnectTask extends NIOTask {

		private ConnectTask(NIOHandler handler, SelectionKey sk) {
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

	private class ReadTask extends NIOTask {

		private ReadTask(NIOHandler handler, SelectionKey sk) {
			super(handler, sk);
		}

		@Override
		public void run() {
			try {
				handler.read(sk);
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

	private class WriteTask extends NIOTask {

		private WriteTask(NIOHandler handler, SelectionKey sk) {
			super(handler, sk);
		}

		@Override
		public void run() {
			try {
				handler.write(sk);
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

	public void run() {
		while(true) {
			changeEvent();
			int select = 0;
			try {
				select = selector.select();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			logger.debug("select is {}", select);
			if (select > 0) {
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					NIOHandler handler = null;
					SelectionKey sk = null;
					try {
						sk = it.next();
						if (!sk.isValid()) {
							sk.cancel();
							continue;
						}
						logger.debug("key op {}", sk.readyOps());
						handler = (NIOHandler) sk.attachment();
						if (sk.isConnectable()) {
							reactor.submit(new ConnectTask(handler, sk));
							sk.cancel();
						} else if (sk.isWritable()) {
							reactor.submit(new WriteTask(handler, sk));
							sk.cancel();
						} else if (sk.isReadable()) {
							reactor.submit(new ReadTask(handler, sk));
							sk.cancel();
						} else {
							sk.cancel();
						}
					} catch (Exception e) {
						if (handler != null) {
							handler.close();
						}
						if (sk != null) {
							sk.cancel();
						}
						e.printStackTrace();
					} finally {
						it.remove();
					}
				}
			}
		}
	}
}
