import com.google.common.collect.Lists;
import com.immomo.exchange.client.Connection;
import com.immomo.exchange.client.ConnectionPool;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by wudikua on 2016/4/10.
 */
public class ConnectionPoolTest {
	@Test
	public void testMultiGet() throws MalformedURLException, InterruptedException {
		final ConnectionPool pool = new ConnectionPool();
		pool.add(new Connection(new URL("http://127.0.0.1"), null));
		ArrayList<Thread> t = Lists.newArrayList();
		for (int i=0;i<10;i++) {
			new Thread(new Runnable() {
				public void run() {
					Connection c = pool.get(ConnectionPool.getKey("127.0.0.1", 80));
					System.out.println(c);
				}
			}).start();
		}
		synchronized (this) {
			this.wait();
		}
	}
}
