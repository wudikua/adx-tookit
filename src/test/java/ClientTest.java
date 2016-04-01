import com.immomo.exchange.client.Client;
import org.junit.Test;

import java.net.MalformedURLException;

/**
 * Created by mengjun on 16/4/1.
 */
public class ClientTest {
	private static final String url = "http://www.baidu.com";

	@Test
	public void get() throws MalformedURLException {
		new Client().get(url);
	}

}
