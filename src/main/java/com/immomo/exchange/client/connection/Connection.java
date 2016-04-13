package com.immomo.exchange.client.connection;

import com.immomo.exchange.client.nio.MultiThreadSelector;
import com.immomo.exchange.client.protocal.Request;
import com.immomo.exchange.client.protocal.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by mengjun on 16/4/1.
 */
public class Connection implements NIOHandler {

	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	private SocketChannel channel;

	private MultiThreadSelector selector;

	private URL url;

	private boolean closed = false;

	private boolean connected = false;

	private Response response = new Response();

	public final Object notify = new Object();

	private String cacheKey;

	@Override
	public String toString() {
		return "Connection{" +
				"channel=" + channel +
				'}';
	}

	public Connection(URL url, MultiThreadSelector selector) {
		this.url = url;
		this.selector = selector;
		this.cacheKey = ConnectionPool.getKey(url.getHost(), url.getPort());
	}

	public boolean prepareConnect() throws IOException {
		if (connected) {
			selector.register(channel, SelectionKey.OP_WRITE, this);
			return true;
		}
		channel = SocketChannel.open();
		channel.configureBlocking(false);
		int port = url.getPort();
		if (port <= 0) {
			port = 80;
		}
		channel.connect(new InetSocketAddress(url.getHost(), port));
		selector.register(channel, SelectionKey.OP_CONNECT, this);
		return true;
	}

	@Override
	public boolean connect(SelectionKey sk) throws IOException {
		if (closed) {
			return false;
		}
		if (!connected) {
			connected = channel.finishConnect();
			sk.interestOps(SelectionKey.OP_WRITE);
		}
		return connected;
	}

	@Override
	public void write(SelectionKey sk) {
		if (closed) {
			return;
		}
		logger.debug("write data");
		ByteBuffer buffer = ByteBuffer.wrap(Request.buildRequest(url));
		while (buffer.hasRemaining()) {
			try {
				channel.write(buffer);
			} catch (IOException e) {
				logger.error("write error, close connection", e);
				close();
			}
		}
		sk.interestOps(SelectionKey.OP_READ);
	}

	@Override
	public void read(SelectionKey sk) {
		if (closed) {
			return;
		}
		logger.debug("read data");
		if (response == null) {
			response = new Response();
		}
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		try {
			int len = 0;
			while((len = channel.read(buffer)) > 0) {
				buffer.flip();
				response.parse(buffer.array(), len);
				buffer.clear();
			}
		} catch (Exception e) {
			logger.error("read error, close connection", e);
			close();
		} finally {
			if (response.finish()) {
				futureFinish();
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
			futureFinish();
			channel.close();
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
}