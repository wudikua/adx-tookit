import com.immomo.exchange.client.protocal.Response;
import org.junit.Test;

import java.io.PrintStream;

/**
 * Created by mengjun on 16/4/13.
 */
public class ResponseTest {
	@Test
	public void testParse() {
		Response r = new Response();
		String s = "HTTP/1.1 200 OK\nConnection:keep-alive\nServer: TimeServer\nContent-Type: text/html; charset=UTF-8\n" +
				"Content-Transfer: chunked\n" + "\n" + "d\n" + System.currentTimeMillis() + "\n" + "0\n\n";
		r.parse(s.getBytes(), s.getBytes().length);
	}
}
