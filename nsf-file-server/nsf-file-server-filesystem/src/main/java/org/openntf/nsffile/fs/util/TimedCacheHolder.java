package org.openntf.nsffile.fs.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to hold a cache map that expires based on a last-modification date.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class TimedCacheHolder {
	private long lastModified = -1;
	private Map<String, Object> cache;
	
	public synchronized Map<String, Object> get(long modTime) {
		if(this.cache == null || this.lastModified == -1 || modTime > this.lastModified) {
			this.cache = new HashMap<>();
			this.lastModified = modTime;
		}
		return this.cache;
	}
}
