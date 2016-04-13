package com.immomo.exchange.client.protocal;

import java.util.Arrays;
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

	private int chunkLength = -1;

	private int pos;

	private StringBuilder key = new StringBuilder();

	private StringBuilder val = new StringBuilder();

	private enum ParseStat {
		NONE, CODE, HEADER_PRE, HEADER_KEY, HEADER_VAL, HEADER_CONTENT_LENGTH, HEADER_CHUNK_LENGTH, BODY, CHUNK, CHUNK_EOF
	}

	private ParseStat stat = ParseStat.NONE;

	public boolean finish() {
		return (contentLength > 0 && pos == contentLength) || (stat == ParseStat.CHUNK_EOF && chunkLength == Integer.MAX_VALUE);
	}

	public void parse(byte[] bs, int length) {
		for (int i=0; i<length; i++) {
			byte b = bs[i];
			switch (stat) {
				case CHUNK_EOF:
					if (b == '\n') {
						chunkLength = Integer.MAX_VALUE;
					}
					break;
				case CHUNK:
					int copyLength = length - i < chunkLength ? length - i : chunkLength;
					System.arraycopy(bs, i, body, pos, copyLength);
					chunkLength -= copyLength;
					pos += copyLength;
					i += copyLength;
					if (chunkLength == 0) {
						stat = ParseStat.HEADER_CHUNK_LENGTH;
					} else {
						stat = ParseStat.CHUNK;
					}
					break;
				case BODY:
					System.arraycopy(bs, i, body, pos, length - i);
					pos += length - i;
					break;
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
						if (contentLength == -1) {
							stat = ParseStat.HEADER_CHUNK_LENGTH;
						} else {
							stat = ParseStat.BODY;
						}
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
						grow(contentLength);
						key.delete(0, key.capacity());
						val.delete(0, val.capacity());
						break;
					}
					val.append((char)b);
					break;
				case HEADER_CHUNK_LENGTH:
					if (b == ' ' || b == '\r') break;
					if (b == '\n') {
						grow(chunkLength);
						if (chunkLength == 0) {
							stat = ParseStat.CHUNK_EOF;
						} else {
							stat = ParseStat.CHUNK;
						}

						break;
					}
					if (chunkLength < 0) {
						if (b >= 'a') {
							chunkLength = 10 + b - 'a';
						} else {
							chunkLength = b - '0';
						}
					} else {
						if (b >= 'a') {
							chunkLength = chunkLength*16 + 10 + b - 'a';
						} else {
							chunkLength = chunkLength*16 + b - '0';
						}
					}
					break;
			}
		}
	}

	/**
	 * 在之前的容量上扩展cap个字节
	 * @param cap 扩容的数量
	 */
	private void grow(int cap) {
		if (cap == 0) {
			return;
		}
		if (body == null) {
			body = new byte[cap];
			return;
		}
		// overflow-conscious code
		int oldCapacity = body.length;
		int newCapacity = oldCapacity + cap;
		if (newCapacity < 0) {
			if (cap < 0) // overflow
				throw new OutOfMemoryError();
			newCapacity = Integer.MAX_VALUE;
		}
		body = Arrays.copyOf(body, newCapacity);
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
