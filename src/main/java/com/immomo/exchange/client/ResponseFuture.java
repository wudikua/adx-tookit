package com.immomo.exchange.client;

import com.immomo.exchange.client.connection.Connection;
import com.immomo.exchange.client.connection.ConnectionControl;
import com.immomo.exchange.client.protocal.Response;
import com.immomo.exchange.client.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;

/**
 * Created by wudikua on 2016/4/9.
 */
public class ResponseFuture implements Future<Response> {

	private static final Logger logger = LoggerFactory.getLogger(ResponseFuture.class);

	private Connection connection;

	private Condition condition;

	private volatile boolean cancelled = false;

	private volatile boolean done = false;

	private final CountDownLatch begin = new CountDownLatch(1);

	public ResponseFuture(Connection connection) {
		this.connection = connection;
		this.condition = connection.condition;
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		connection.close();
		return true;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public boolean isDone() {
		return done;
	}

	public Response get() throws InterruptedException, ExecutionException {
		logger.debug("get begin {}", System.currentTimeMillis());
		while(!isDone() && !isCancelled()) {
			// 没结束就继续等待
			connection.lock.lock();
			begin.countDown();
			condition.await();
			connection.lock.unlock();
		}
		logger.debug("get end {}", System.currentTimeMillis());
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
		Timer.TimerUnit timer = new Timer.TimerUnit(unit.toMillis(timeout)) {
			@Override
			public void onTime() throws Exception {
				begin.await();
				connection.lock.lock();
				cancelled = true;
				condition.signalAll();
				connection.lock.unlock();
			}
		};
		Timer.addTimeout(timer);
		try {
			return get();
		} finally {
			timer.done = true;
		}
	}
}
