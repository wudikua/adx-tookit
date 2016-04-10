package com.immomo.exchange.client;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.PooledConnection;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by mengjun on 16/4/1.
 */
public class Connection {

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

	public void write(SelectionKey sk) {
		if (closed) {
			return;
		}
		logger.debug("write data");
		String uri = "/";
		if (!url.getPath().equals("")) {
			uri  = url.getPath();
		}
		String request = "GET " + uri + " HTTP/1.1\n" +
				"Connection: keep-alive\n" +
				"Host: "+url.getHost()+"\n" +
				"User-Agent:AdExchange\n\n";
		ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
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

	public void read(SelectionKey sk) {
		if (closed) {
			return;
		}
		logger.debug("read data");
		if (response == null) {
			response = new Response();
		}
		ByteBuffer buffer = ByteBuffer.allocate(8);
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
				finish();
			}
		}
	}

	private void finish() {
		synchronized (notify) {
			notify.notifyAll();
		}
	}

	public void close() {
		try {
			finish();
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
