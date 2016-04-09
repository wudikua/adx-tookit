import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * Created by wudikua on 2016/4/9.
 */
public class TimeServer {
	public static String charset = "utf-8";
	private ServerSocket ss;
	private Socket client;

	public TimeServer() {
		try {
			this.ss = new ServerSocket(80);
			while (true) {
				this.client = this.ss.accept();
				ClientThread ct = new ClientThread(this.client);
				Thread t = new Thread(ct);
				t.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try{
				if(this.ss != null){
					this.ss.close();
					this.ss = null;
				}
				if(this.client != null){
					this.client.close();
					this.client = null;
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

}
