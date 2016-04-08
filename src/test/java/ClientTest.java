import com.immomo.exchange.client.Client;
import com.immomo.exchange.client.MultiThreadSelector;
import org.junit.Test;

import java.net.MalformedURLException;

/**
 * Created by mengjun on 16/4/1.
 */
public class ClientTest {
	private static final String url = "http://www.baidu.com";

	@Test
	public void get() throws Exception {
		MultiThreadSelector selector = new MultiThreadSelector(1);
		selector.init();
		selector.start();
		new Client(selector).get(url);
		new Client(selector).get(url);
		synchronized (this) {
			this.wait();
		}
	}

}
