import com.immomo.exchange.client.util.Timer;
import org.junit.Test;

/**
 * Created by wudikua on 2016/4/10.
 */
public class TimerTest {
	@Test
	public void testTimeout() throws InterruptedException {
		Timer.addTimeout(new Timer.TimerUnit(1000) {
			@Override
			public void onTime() throws Exception {
				System.out.println("timeout");
			}
		});
		Timer.addTimeout(new Timer.TimerUnit(1000) {
			@Override
			public void onTime() throws Exception {
				System.out.println("timeout");
			}
		});

		synchronized (this) {
			this.wait();
		}
	}
}
