package com.immomo.exchange.client.protocal;


import org.slf4j.helpers.MessageFormatter;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Created by mengjun on 16/4/11.
 */
public class Request {

	private static final String template =
			"{} {} HTTP/1.1\nConnection: keep-alive\nHost: {}\nAccept: */*\nUser-Agent: AdExchange\n";

	public static byte[] buildRequest(URL url, String method, Map<String, String> headers, byte[] body) {
		String uri = "/";
		if (!url.getPath().equals("")) {
			uri  = url.getPath();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		try {
			out.write(MessageFormatter.arrayFormat(template, new Object[]{
					method, uri, url.getHost()
			}).getMessage().getBytes());
			out.write("Content-Length: ".getBytes());
			if (body != null) {
				out.write(String.valueOf(body.length).getBytes());
			} else {
				out.write("0".getBytes());
			}
			out.write('\n');

			if (headers != null && headers.size() > 0) {
				for (String key : headers.keySet()) {
					out.write(key.getBytes());
					out.write(": ".getBytes());
					out.write(headers.get(key).getBytes());
					out.write('\n');
				}
			}
			out.write('\n');
			if (body != null) {
				out.write(body);
			}
			out.write('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}

}
