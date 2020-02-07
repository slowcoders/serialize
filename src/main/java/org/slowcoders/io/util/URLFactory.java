package org.slowcoders.io.util;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: InterWise</p>
 * @author unascribed
 * @version 1.0
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLStreamHandler;

import org.slowcoders.util.WeakValueMap;


public final class URLFactory {
	static WeakValueMap<String, URLStreamHandler> handlers = new WeakValueMap<String, URLStreamHandler>();
	
	public static URL makeURL(URL base, String url) throws MalformedURLException {
		int idxColon = url.indexOf(':');
		if (idxColon > 0) {
			String protocol = url.substring(0, idxColon);
			URLStreamHandler handler = handlers.get(protocol);
			if (handler != null) {
				return new URL(base, url, handler);
			}
		}
		else 
		if (base != null && !url.startsWith("/") 
		&&  "content".equals(base.getProtocol())) {
			String s = base.toString();
			int start = s.lastIndexOf('/');
			int m = s.lastIndexOf("%2F");
			if (start > 0 && m > start) {
				s = s.substring(start + 1, m + 3);
				url = s + URLEncoder.encode(url);
			}
		}
		return new URL(base, url);
	}

	public static void addURLStreamHandler(String protocol, URLStreamHandler streamHandler) {
		handlers.put(protocol, streamHandler);
	}

}
