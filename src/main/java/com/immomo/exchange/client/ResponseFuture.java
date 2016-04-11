package com.immomo.exchange.client;

import com.immomo.exchange.client.connection.Connection;
import com.immomo.exchange.client.protocal.Response;
import com.immomo.exchange.client.util.Timer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by wudikua on 2016/4/9.
 */
public class ResponseFuture implements Future<Response> {

	private Connection connection;

	private volatile boolean cancelled = false;

	private volatile boolean done = false;

	public ResponseFuture(Connection connection) {
		this.connection = connection;
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		if (cancelled) {
			return true;
		}
		connection.close();
		return true;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public boolean isDone() {
		return done;
	}

	public Response get() throws InterruptedException, ExecutionException {
		synchronized (connection.notify) {
			connection.notify.wait();
		}
		done = true;
		Response response =  connection.getResponse();
		if (response == null) {
			throw new ExecutionException(new Exception("request fail"));
		}
		if (response.finish()) {
			Client.pool.add(connection);
			return response;
		} else {
			return null;
		}
	}

	public Response get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		Timer.addTimeout(new Timer.TimerUnit(unit.toMillis(timeout)) {
			@Override
			public void onTime() throws Exception {
				synchronized (connection.notify) {
					connection.notify.notifyAll();
				}
			}
		});
		return get();
	}
}
