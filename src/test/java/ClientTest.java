import com.immomo.exchange.client.Client;
import com.immomo.exchange.client.MultiThreadSelector;
import com.immomo.exchange.client.Response;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by mengjun on 16/4/1.
 */
public class ClientTest {

	private static final String url = "http://lian.us";

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
	public void get() throws Exception {
		for (int i=0; i<10; i++) {
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
			System.out.println(System.currentTimeMillis()/1000 + " " + new String(resp.getBody()).substring(0, 10) + "...");
		}
//		Thread.sleep(1000);
	}

	@Test
	public void server() {
		new TimeServer();
	}

}
