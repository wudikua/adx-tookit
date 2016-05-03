package com.immomo.exchange.client.connection;

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

	public final Object notify = new Object();

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

	public Connection(URL url, SingleThreadSelector selector) {
		this.url = url;
		this.selector = selector;
		this.cacheKey = ConnectionPool.getKey(url.getHost(), url.getPort());
	}

	public boolean prepareConnect() throws IOException {
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
		this.sk = sk;
		if (closed) {
			return false;
		}
		if (!connected) {
			connected = channel.finishConnect();
			selector.register(channel, SelectionKey.OP_WRITE, this, sk);
		}
		return connected;
	}

	@Override
	public void write() {
		if (closed) {
			sk.cancel();
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
				sk.cancel();
				close();
				return;
			}
		}
		selector.register(channel, SelectionKey.OP_READ, this, sk);
	}

	@Override
	public void read() {
		if (closed) {
			sk.cancel();
			return;
		}
		logger.debug("read data");
		if (response == null) {
			response = new Response();
		}
		ByteBuffer buffer = ByteBuffer.allocate(1024*4);
		try {
			int len;
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
			if (response.finish()) {
				futureFinish();
				// 不再继续监听读写事件了
				selector.register(channel, SelectionKey.OP_READ, this, sk);
			} else if (!closed) {
				// 还有数据需要读取
				try {
					// 当服务端主动关闭连接以后，客户端如果不判断available会一直read 0字节
					if (channel.socket().getInputStream().available() > 0) {
						selector.register(channel, SelectionKey.OP_READ, this, sk);
					} else {
						sk.cancel();
						close();
					}
				} catch (IOException e) {
					sk.cancel();
					e.printStackTrace();
				}
			} else {
				// future已经超时，不继续监听事件循环
				sk.cancel();
			}
		}
	}

	private void futureFinish() {
		synchronized (notify) {
			notify.notifyAll();
		}
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		try {
			ConnectionControl.release(cacheKey);
			futureFinish();
			if (channel != null) {
				channel.close();
			}
			closed = true;
		} catch (IOException e) {
			logger.error("close error", e);
		}
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
}
