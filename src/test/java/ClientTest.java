import com.immomo.exchange.client.Client;
import com.immomo.exchange.client.MultiThreadSelector;
import com.immomo.exchange.client.Response;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

/**
 * Created by mengjun on 16/4/1.
 */
public class ClientTest {
	private static final String url = "http://127.0.0.1";

	@Test
	public void get() throws Exception {
		MultiThreadSelector selector = new MultiThreadSelector(1);
		selector.init();
		selector.start();
		Client client = new Client(selector);
		for (int i=0; i<10; i++) {
			Future<Response> future = client.get(url);
			Response resp = future.get();
			System.out.println(new String(resp.getBody()));

			Thread.sleep(1000);
		}
	}

	@Test
	public void server() {
		new TimeServer();
	}

}
