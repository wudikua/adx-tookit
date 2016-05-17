package com.immomo.exchange.client;

/**
 * Created by mengjun on 16/4/1.
 */
public class Config {
	public static int maxConnections;
	public static int maxConnectionTime;
	public static int connectionExpireTime;
	public static int globalTimeout;

	static {
		maxConnections = 10000;
		maxConnectionTime = 20 * 1000;
		connectionExpireTime = 10 * 1000;
		globalTimeout = 30000;
	}
}
