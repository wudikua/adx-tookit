package com.immomo.exchange.client;

import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wudikua on 2016/4/9.
 */
public class Response {

	private int status = 0;

	private Map<String, String> header = new HashMap<String, String>();

	private byte[] body;

	private int contentLength = -1;

	private int pos;

	private StringBuilder key = new StringBuilder();

	private StringBuilder val = new StringBuilder();

	private enum ParseStat {
		NONE, CODE, HEADER_PRE, HEADER_KEY, HEADER_VAL, HEADER_CONTENT_LENGTH, BODY
	}

	private ParseStat stat = ParseStat.NONE;

	public boolean finish() {
		return contentLength > 0 && pos == contentLength;
	}

	public void parse(byte[] bs, int length) {
		for (int i=0; i<length; i++) {
			if (stat == ParseStat.BODY) {
				System.arraycopy(bs, i, body, pos, length - i - 2);
				pos += length - i -2;
				break;
			}
			byte b = bs[i];
			switch (stat) {
				case NONE:
					if (b == ' ') {
						stat = ParseStat.CODE;
					}
					break;
				case CODE:
					if (b == ' ') {
						stat = ParseStat.HEADER_PRE;
						break;
					}
					status = status*10 + b - '0';
					break;
				case HEADER_PRE:
					if (b == '\n') {
						stat = ParseStat.HEADER_KEY;
					}
					break;
				case HEADER_KEY:
					if (b == '\n') {
						stat = ParseStat.BODY;
						break;
					}
					if (b == ' ' || b == '\r') {
						break;
					}
					if (b == ':') {
						stat = ParseStat.HEADER_VAL;
						break;
					}
					key.append((char)b);
					break;
				case HEADER_VAL:
					if (key.toString().equals("Content-Length")) {
						stat = ParseStat.HEADER_CONTENT_LENGTH;
						break;
					}
					if (b == ' ' || b == '\r') {
						break;
					}
					if (b == '\n') {
						stat = ParseStat.HEADER_KEY;
						header.put(key.toString(), val.toString());
						key.delete(0, key.capacity());
						val.delete(0, val.capacity());
						break;
					}
					val.append((char)b);
					break;
				case HEADER_CONTENT_LENGTH:
					if (b == ' ' || b == '\r') break;
					if (b == '\n') {
						stat = ParseStat.HEADER_KEY;
						header.put(key.toString(), val.toString());
						contentLength = Integer.parseInt(val.toString());
						this.body = new byte[contentLength];
						key.delete(0, key.capacity());
						val.delete(0, val.capacity());
						break;
					}
					val.append((char)b);
					break;
				case BODY:
					break;
			}
		}
	}

	public void reset() {
		key = new StringBuilder();
		val = new StringBuilder();
		body = null;
		header = new HashMap<String, String>();
		status = 0;
		stat = ParseStat.NONE;
	}

	public int getStatus() {
		return status;
	}

	public Map<String, String> getHeader() {
		return header;
	}

	public byte[] getBody() {
		return body;
	}

	public int getContentLength() {
		return contentLength;
	}

}
