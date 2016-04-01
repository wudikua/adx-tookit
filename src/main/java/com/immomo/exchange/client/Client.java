package com.immomo.exchange.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

/**
 * Created by mengjun on 16/4/1.
 */
public class Client {

	public void get(String url) throws Exception {
		URL parsed = new URL(url);
		Connection conn = new Connection(parsed.getHost(), parsed.getPort());

	}

}
