package com.immomo.exchange.client;

/**
 * Created by mengjun on 16/4/1.
 */
public class Config {
	// 最大连接
	public static int maxConnections;
	// 每个域名最大连接
	public static int maxPerHostConnections;
	// 连接最长连接时间
	public static int maxConnectionTime;
	// 连接多久没被用过以后过期
	public static int connectionExpireTime;

	static {
		maxConnections = 10000;
		maxPerHostConnections = 300;
		// 一个连接最多存活1小时
		maxConnectionTime = 3600 * 1000;
		// 10分钟没人用这个链接就回收掉
		connectionExpireTime = 10 * 1000;
	}
}
