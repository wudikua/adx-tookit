import com.immomo.exchange.client.Client;
import com.immomo.exchange.client.nio.MultiThreadSelector;
import com.immomo.exchange.client.protocal.Response;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by mengjun on 16/4/1.
 */
public class ClientTest {

	private static final String url = "http://www.baidu.com";

	private static Client client;

	static {
		MultiThreadSelector selector = new MultiThreadSelector(1);
		try {
			selector.init();
			selector.start();
			client = new Client(selector);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	public void get() throws Exception {
		for (int i=0; i<1; i++) {
			new Thread(new Runnable() {
				public void run() {
					for (int i = 0; i < 10; i++) {
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
		Thread.sleep(1000);
	}

	@Test
	public void server() {
		new TimeServer();
	}

}
