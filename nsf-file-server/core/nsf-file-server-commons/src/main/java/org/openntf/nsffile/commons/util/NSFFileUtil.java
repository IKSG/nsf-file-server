/**
 * Copyright (c) 2019-2024 Jesse Gallagher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openntf.nsffile.commons.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.commons.util.PathUtil;
import com.ibm.commons.util.StringUtil;

import lotus.domino.Name;

/**
 * Common utilities for working with NSF-based filesystems
 * 
 * @since 2.0.0
 */
public enum NSFFileUtil {
	;
	
	private static final Logger log = Logger.getLogger(NSFFileUtil.class.getPackage().getName());
	
	/**
	 * <p>Takes an Domino-format name and converts it to LDAP format.</p>
	 * 
	 * <p>If the provided value is not a valid Domino name, the original value is returned.</p>
	 */
	public static String dominoNameToLdap(String value) {
		// There's not a convenient class handy for this
		// TODO see if the ODA stuff can be co-opted
		try {
			if(StringUtil.isEmpty(value)) {
				return value;
			} else if(!value.contains("/")) { //$NON-NLS-1$
				if(!value.contains("=")) { //$NON-NLS-1$
					return "cn=" + value; //$NON-NLS-1$
				} else {
					// Then it should be an LDAP-type name already
					return value;
				}
			}
			return NotesThreadFactory.call(session -> {
				Name name = session.createName(value);
				try {
					String dn = name.getCanonical();
					if(!dn.contains("=")) { //$NON-NLS-1$
						return dn;
					}
					StringBuilder result = new StringBuilder();
					for(String component : dn.split("/")) { //$NON-NLS-1$
						if(result.length() > 0) {
							result.append(',');
						}
						int indexEq = component == null ? -1 : component.indexOf('=');
						if(component != null && indexEq > -1) {
							result.append(component.substring(0, indexEq).toLowerCase());
							result.append('=');
							result.append(component.substring(indexEq+1));
						} else {
							result.append(component);
						}
					}
					return result.toString();
				} finally {
					name.recycle();
				}
			});
		} catch(Exception e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception converting Domino name to LDAP: {0}", value), e);
			}
			throw new RuntimeException(e);
		}
	}

	public static String normalizeReplicaID(String id) {
		String replicaId = id;
		if (StringUtil.isNotEmpty(replicaId)) {
			if (replicaId.indexOf(':') == 8 && replicaId.length() == 17) {
				replicaId = replicaId.substring(0, 8) + replicaId.substring(9, 17);
			}
	
			if (replicaId.length() == 16) {
				return replicaId.toUpperCase();
			}
		}
	
		return null;
	}

	public static boolean isReplicaID(String dbPath) {
		String id = normalizeReplicaID(dbPath);
		if (id == null) {
			return false;
		} else {
			for (int i = 0; i < 16; ++i) {
				if ("0123456789ABCDEF".indexOf(id.charAt(i)) < 0) { //$NON-NLS-1$
					return false;
				}
			}
	
			return true;
		}
	}

	public static String dn(String name) {
		return NotesThreadFactory.call(session -> session.createName(name).getCanonical());
	}

	public static String shortCn(String name) {
		return NotesThreadFactory.call(session -> {
			Name n = session.createName(name);
			try {
				return n.getCommon().replaceAll("\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
			} finally {
				n.recycle();
			}
		});
	}

	public static String concat(final char delim, final String... parts) {
		if(parts == null || parts.length == 0) {
			return StringUtil.EMPTY_STRING;
		}
		String path = parts[0];
		for(int i = 1; i < parts.length; i++) {
			path = PathUtil.concat(path, parts[i], delim);
		}
		return path;
	}

	public static String concat(final String... parts) {
		return concat('/', parts);
	}
}
