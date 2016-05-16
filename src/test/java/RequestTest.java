import com.immomo.exchange.client.protocal.Request;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by mengjun on 16/4/11.
 */
public class RequestTest {
	@Test
	public void testRequestBuilder() throws MalformedURLException {
		URL u  = new URL("http://www.baidu.com");
		System.out.println(new String(Request.buildRequest(u, "GET", null, null)));
	}
}
