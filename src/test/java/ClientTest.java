import com.google.common.collect.Lists;
import com.immomo.exchange.client.Client;
import com.immomo.exchange.client.ThirdHttpClient;
import com.immomo.exchange.client.nio.SingleThreadSelector;
import com.immomo.exchange.client.protocal.Response;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mengjun on 16/4/1.
 */
public class ClientTest {

	private static final String url = "http://127.0.0.1/index.html";
//	private static final String url = "http://m.baidu.com";
	private static final String postUrl = "http://api.w.inmobi.com/showad/v2.1";
	private static final String postData = "{\"responseformat\":\"native\",\"imp\":[{\"ads\":1}],\"site\":{\"id\":\"1bbb4214e04c4da482a767ec9ad24add\"},\"device\":{\"ida\":\"96CF2863-9603-44F8-8D9A-45D3838A92A7\",\"ip\":\"123.125.212.11\",\"connectiontype\":\"wifi\",\"geo\":{\"lat\":\"39.9971\",\"lon\":\"116.4802\",\"accu\":\"\"},\"ua\":\"Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13E238\"},\"user\":{\"gender\":\"m\",\"yob\":\"1989\"}}";

	private static Client client;

	static {
		List<SingleThreadSelector> selectors = Lists.newArrayList();
		try {
			for (int i=0;i<Runtime.getRuntime().availableProcessors();i++) {
				SingleThreadSelector selector = new SingleThreadSelector();
				selector.init();
				selector.start();
				selectors.add(selector);
			}
			client = new Client(selectors);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPost() throws Exception {
		System.out.println(
		ThirdHttpClient.client.preparePost(postUrl).setBody(postData).addHeader("Content-Type", "application/json").execute().get().getResponseBody());
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		Future<Response> f = client.execute("POST", postUrl, postData.getBytes(), headers);
		Response r = f.get();
		System.out.println(r);
		System.out.println(r.getStatus());
		System.out.println(new String(r.getBody()));
	}

	@Test
	public void getTimeout() throws Exception {
		Future<Response> future = client.get(url);
		Response resp = future.get(10, TimeUnit.MILLISECONDS);
		if (resp == null) {
			future.cancel(true);
			System.out.println("null");
		} else {
			System.out.println(System.currentTimeMillis()/1000 + " " + new String(resp.getBody()).substring(0, 10) + "...");
		}
		Thread.sleep(1000);
	}

	@Test
	public void testQueue() {
		TreeMap<Integer, Integer> s = new TreeMap<Integer, Integer>();
		s.put(1, 1);
		s.put(2, 2);
		s.put(3, 3);
		System.out.println(s.pollFirstEntry());

	}

	@Test
	public void hex() {
		String s = "17ffe";
		int n=0;
		for (int i=0; i<s.length(); i++) {
			if (s.charAt(i) >= 'a') {
				n = n*16 + 10 + s.charAt(i) - 'a';
			} else {
				n = n*16 + s.charAt(i) - '0';
			}
		}
		System.out.println(n);
	}

	@Test
	public void getOne() throws Exception {
		request();
		synchronized (ClientTest.class) {
			ClientTest.class.wait();
		}
	}

	@Test
	public void abNing() throws InterruptedException {
		final int N = 1000;
		final CountDownLatch finish = new CountDownLatch(N);
		final Long begin = System.currentTimeMillis();
		final AtomicLong success = new AtomicLong(0);
		final AtomicLong request = new AtomicLong(0);
		for (int i=0; i<N; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int j =0; j<100; j++) {
						try {
							request.incrementAndGet();
							Future f = ThirdHttpClient.client.prepareGet(url).execute();
							com.ning.http.client.Response r = ThirdHttpClient.waitResponse(f, 1000);
							if (r.getStatusCode() == 200) {
//								System.out.println("thread " + Thread.currentThread() + " success");
								success.incrementAndGet();
							}
							if (request.get() % 1000 == 0) {
								Long end = System.currentTimeMillis();
								System.out.println("thread " + Thread.currentThread() + " current qps is " + request.get()*1000/(end - begin) + " request " + request.get() + " success " + success.get());
							}
						} catch (Exception e) {

						}
					}
					finish.countDown();
				}
			}).start();
		}
		finish.await();
		Long end = System.currentTimeMillis();
		System.out.println("time total use " + (end - begin) + " request " + request.get() + " success " + success.get());
	}

	@Test
	public void ab() throws InterruptedException {
		final int N = 1000;
		final CountDownLatch finish = new CountDownLatch(N);
		final Long begin = System.currentTimeMillis();
		final AtomicLong success = new AtomicLong(0);
		final AtomicLong request = new AtomicLong(0);
		for (int i=0; i<N; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int j =0; j<100; j++) {
						try {
							request.incrementAndGet();
							Future<Response> future = client.get(url);
							Response resp = future.get(1, TimeUnit.SECONDS);
							if (resp.getStatus() == 200) {
								success.incrementAndGet();
//								System.out.println(new String(resp.getBody()));
							} else {
								System.out.println("timeout");
							}
						} catch (Exception e) {
//							e.printStackTrace();
						}
						if (request.get()%1000 == 0) {
							Long end = System.currentTimeMillis();
							System.out.println("thread " + Thread.currentThread() + " current qps is " + request.get()*1000/(end - begin) + " request " + request.get() + " success " + success.get());
						}
					}
					finish.countDown();
				}
			}).start();
		}
		finish.await();
		Long end = System.currentTimeMillis();
		System.out.println("time total use " + (end - begin) + " request " + request.get() + " success " + success.get());
	}

	@Test
	public void testTimePerformance() {
		Long b = System.currentTimeMillis();
		for (int i=0; i<100000; i++) {
			System.currentTimeMillis();
		}
		System.out.println((System.currentTimeMillis() - b));
	}

	@Test
	public void get() throws Exception {
		for (int i=0; i<10; i++) {
			new Thread(new Runnable() {
				public void run() {
					for (int i = 0; i < 1; i++) {
						try {
							request();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					synchronized (ClientTest.class) {
						ClientTest.class.notifyAll();
					}
				}
			}).start();
		}
		synchronized (ClientTest.class) {
			ClientTest.class.wait();
		}
	}


	private void request() throws Exception {
		Future<Response> future = client.get(url);
		Response resp = future.get();
		if (resp == null) {
			future.cancel(true);
			System.out.println("null");
		} else {
			System.out.println(System.currentTimeMillis()/1000 + " " + new String(resp.getBody()).substring(0, 13) + "...");
		}
//		Thread.sleep(1000);
	}

	@Test
	public void server() {
		new TimeServer();
	}

}
