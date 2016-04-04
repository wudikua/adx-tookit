package com.immomo.exchange.client;

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

	private SocketChannel channel;

	private Selector selector;

	private URL url;

	private boolean closed = false;

	private boolean connected = false;

	public Connection(URL url, Selector selector) {
		this.url = url;
		this.selector = selector;
	}

	public boolean connect() throws IOException {
		if (!connected) {
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_WRITE, this);
			channel.connect(new InetSocketAddress(url.getHost(), url.getPort()));
			connected = channel.finishConnect();
		}
		return connected;
	}

	public void write() {
		String request = "GET " + url.getPath() + " HTTP/1.1\nConnection: Keep-Alive\n";
		ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
		while (buffer.hasRemaining()) {
			try {
				channel.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			channel.register(selector, SelectionKey.OP_READ, this);
		} catch (ClosedChannelException e) {
			e.printStackTrace();
		}
	}

	public void read() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		try {
			while(channel.read(buffer) > 0) {
				System.out.println(new String(buffer.array()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
	}
}
