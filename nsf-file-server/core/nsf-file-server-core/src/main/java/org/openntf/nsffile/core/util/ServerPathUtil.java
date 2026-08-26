package org.openntf.nsffile.core.util;

import java.nio.file.Path;

public enum ServerPathUtil {
	;
	
	public static String toFileName(Path path) {
		String p = path.toString();
		if(p.endsWith("/.") && p.length() > 3) { //$NON-NLS-1$
			p = p.substring(0, p.length()-2);
		}
		return p;
	}
}
