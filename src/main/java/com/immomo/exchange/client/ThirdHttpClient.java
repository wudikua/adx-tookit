package com.immomo.exchange.client;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

import java.util.concurrent.*;

/**
 * Created by mengjun on 16/4/20.
 */
public class ThirdHttpClient {

	public static AsyncHttpClient client;


	static {
		ExecutorService executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
				Runtime.getRuntime().availableProcessors() * 4, 10 * 1000,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(10 * 1000)
		);

		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
				.setExecutorService(executor)
				.setReadTimeout(3000)
				.setConnectTimeout(3000)
				.setRequestTimeout(3000)
				.setMaxConnections(10000)
				.setMaxConnectionsPerHost(1000)
				.build();
		client = new AsyncHttpClient(config);
	}

	public static Response waitResponse(Future<Response> future, int timeout) {
		Response response = null;
		try {
			response = future.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
//			e.printStackTrace();
		} catch (ExecutionException e) {
//			e.printStackTrace();
		} catch (TimeoutException e) {
//			e.printStackTrace();
		}
		if (response == null) {
			future.cancel(true);
		}
		return response;
	}

}
