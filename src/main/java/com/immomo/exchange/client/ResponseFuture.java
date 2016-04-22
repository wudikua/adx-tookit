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
		System.out.println("get beign " + System.currentTimeMillis());
		synchronized (connection.notify) {
			connection.notify.wait();
		}
		System.out.println("get end " + System.currentTimeMillis());
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
		if (unit == null) {
			throw new InterruptedException("unit is null");
		}
		Timer.addTimeout(new Timer.TimerUnit(unit.toMillis(timeout)) {
			@Override
			public void onTime() throws Exception {
				Long begin = System.currentTimeMillis();
				synchronized (connection.notify) {
					connection.notify.notifyAll();
				}
				Long end = System.currentTimeMillis();
				System.out.println("timeout notify use " + (end - begin));
			}
		});
		return get();
	}
}
