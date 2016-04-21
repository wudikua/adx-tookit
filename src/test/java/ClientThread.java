/**
 * Created by wudikua on 2016/4/9.
 */
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ClientThread implements Runnable {

	private Socket _s;
	private InputStream i;
	private PrintStream o;

	public ClientThread(Socket s) {
		this._s = s;
	}

	public void run() {
		try {
			i = _s.getInputStream();
			o = new PrintStream(_s.getOutputStream());
			while(true) {
				String request = this.getValidRequest();
				if( !request.equals("") && request.length() > 0) {
					System.out.println(System.currentTimeMillis() + " req " + request);
					o.println("HTTP/1.1 200 OK");
					o.println("Connection:keep-alive");
					o.println("Server: TimeServer");
					o.println("Content-Type: text/html; charset=UTF-8");
					o.println("Content-Transfer: chunked");
//					o.println("Content-Length: " + String.valueOf(System.currentTimeMillis()).length());
					o.println();
					o.println("d");
					o.println(System.currentTimeMillis());
					o.println("0");
					o.flush();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			System.out.println("connection closed");
			this.close();
		}
	}

	private String getValidRequest() throws InterruptedException, IOException {
		StringBuilder request = new StringBuilder();
		byte[] buff = new byte[16];
		while (i.read(buff) > 0 && i.available() != 0) {
			request.append(Arrays.toString(buff));
		}
		return request.toString();
	}

	private void close() {
		try {
			if (i != null) {
				i.close();
				i = null;
			}
			if (o != null) {
				o.close();
				o = null;
			}
			if (_s != null) {
				_s.close();
				_s = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
