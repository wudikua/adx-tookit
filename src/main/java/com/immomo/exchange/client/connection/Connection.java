package com.immomo.exchange.client.connection;

import com.immomo.exchange.client.ResponseFuture;
import com.immomo.exchange.client.nio.SingleThreadSelector;
import com.immomo.exchange.client.protocal.Request;
import com.immomo.exchange.client.protocal.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mengjun on 16/4/1.
 */
public class Connection implements NIOHandler {

	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	private SocketChannel channel;

	private SingleThreadSelector selector;

	private URL url;

	private volatile boolean closed = false;

	private boolean connected = false;

	private Response response = new Response();

	private ResponseFuture future;

	public final Lock lock = new ReentrantLock();

	public final Condition condition = lock.newCondition();

	private String cacheKey;

	private long lastActiveTime = System.currentTimeMillis();

	private long createTime = System.currentTimeMillis();

	private SelectionKey sk;

	@Override
	public String toString() {
		return "Connection{" +
				"channel=" + channel +
				'}';
	}

	public Connection(SingleThreadSelector selector) {
		this.selector = selector;
	}

	public boolean prepareConnect(URL url) throws IOException {
		this.url = url;
		this.cacheKey = ConnectionPool.getKey(url.getHost(), url.getPort());
		if (closed) {
			return false;
		}
		if (connected) {
			selector.register(channel, SelectionKey.OP_WRITE, this, sk);
			return true;
		}
		channel = SocketChannel.open();
		channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
		channel.configureBlocking(false);
		int port = url.getPort();
		if (port <= 0) {
			port = 80;
		}
		channel.connect(new InetSocketAddress(url.getHost(), port));
		selector.register(channel, SelectionKey.OP_CONNECT, this, null);
		return true;
	}

	@Override
	public boolean connect(SelectionKey sk) throws IOException {
		if (closed) {
			return false;
		}
		this.sk = sk;
		if (!connected) {
			connected = channel.finishConnect();
			if (connected) {
				selector.register(channel, SelectionKey.OP_WRITE, this, sk);
			} else {
				logger.error("connection establish fail");
				close();
			}
		}
		return connected;
	}

	@Override
	public void write() {
		if (closed) {
			return;
		}
		logger.debug("write data");
		ByteBuffer buffer = ByteBuffer.wrap(Request.buildRequest(url));
		while (buffer.hasRemaining()) {
			try {
				if (closed) {
					// 防止hasRemaining死循环
					sk.cancel();
					return;
				}
				if (channel.write(buffer) < 0) {
					// broken pipe?
					throw new IOException("write return less than 0");
				}
			} catch (IOException e) {
				logger.error("write error, close connection", e);
				close();
				return;
			}
		}
		selector.register(channel, SelectionKey.OP_READ, this, sk);
	}

	@Override
	public void read() {
		if (closed) {
			return;
		}
		logger.debug("read data");
		if (response == null) {
			response = new Response();
		}
		ByteBuffer buffer = ByteBuffer.allocate(1024*4);
		int len = 0;
		try {
			while((len = channel.read(buffer)) > 0) {
				buffer.flip();
				response.parse(buffer.array(), len);
				buffer.clear();
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("read error, close connection", e);
			sk.cancel();
			close();
		} finally {
			if (response != null && response.finish()) {
				futureFinish(true);
				// 不再继续监听读写事件了
				selector.register(channel, SelectionKey.OP_READ, this, sk);
			} else if (!closed) {
				// 还有数据需要读取
				// 当服务端主动关闭连接以后，客户端如果不判断available会一直read 0字节
				if (len >= 0) {
					selector.register(channel, SelectionKey.OP_READ, this, sk);
				} else {
					logger.error("socket is not available close connection, read {}", len);
					close();
				}
			} else {
				// future已经超时，不继续监听事件循环
				logger.error("future is timeout");
				close();
			}
		}
	}

	private void futureFinish(boolean isDone) {
		try {
			lock.lock();
			if (isDone) {
				future.setDone(true);
			} else {
				future.setCancelled(true);
			}
			condition.signalAll();
			lock.unlock();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		try {
			futureFinish(false);
			if (channel != null) {
				channel.close();
			}
			closed = true;
			if (sk != null && sk.isValid()) {
				sk.cancel();
			}
		} catch (IOException e) {
			logger.error("close error", e);
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	public Response getResponse() {
		Response r = response;
		response = null;
		return r;
	}

	public String getCacheKey() {
		return cacheKey;
	}

	public void updateActiveTime() {
		lastActiveTime = System.currentTimeMillis();
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public long getCreateTime() {
		return createTime;
	}

	public Future<Response> getFuture() {
		return future;
	}

	public void setFuture(ResponseFuture future) {
		this.future = future;
	}
}
