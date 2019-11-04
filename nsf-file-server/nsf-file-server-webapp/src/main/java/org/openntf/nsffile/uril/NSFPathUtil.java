package org.openntf.nsffile.uril;

import java.net.URI;
import java.util.Objects;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public enum NSFPathUtil {
	;
	
	public static String extractApiPath(URI uri) {
		Objects.requireNonNull(uri, "uri cannot be null");
		
		String host = uri.getHost();
		String pathInfo = uri.getPath();
		if(pathInfo == null || pathInfo.isEmpty()) {
			throw new IllegalArgumentException("URI path info cannot be empty");
		}
		
		int nsfIndex = pathInfo.toLowerCase().indexOf(".nsf");
		if(nsfIndex < 2) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri);
		}
		String nsfPath = pathInfo.substring(1, nsfIndex+4);
		if(host == null || host.isEmpty()) {
			return nsfPath;
		} else {
			return host + "!!" + nsfPath;
		}
	}
}
