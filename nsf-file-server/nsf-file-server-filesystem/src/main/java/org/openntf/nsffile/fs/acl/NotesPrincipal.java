package org.openntf.nsffile.fs.acl;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.openntf.nsffile.util.NotesThreadFactory;

import com.ibm.commons.util.StringUtil;

import lotus.domino.Name;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NotesPrincipal implements UserPrincipal, GroupPrincipal {
	
	public static NotesPrincipal fromLdap(String dn) {
		return new NotesPrincipal(ldapNameToDomino(dn));
	}
	
	private final LdapName ldapName;
	
	public NotesPrincipal(String dominoName) {
		try {
			this.ldapName = new LdapName(dominoNameToLdap(dominoName));
		} catch (InvalidNameException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return ldapName.toString();
	}
	
	@Override
	public String toString() {
		return getName();
	}

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
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * <p>Takes an LDAP-format distinguished name and converts it to Domino format.</p>
	 * 
	 * <p>If the provided value is not a valid LDAP name, the original value is returned.</p>
	 */
	public static String ldapNameToDomino(String value) {
		if(StringUtil.isEmpty(value)) {
			return ""; //$NON-NLS-1$
		} else {
			// Make sure it's actually an LDAP name. We'll assume that an un-escaped slash is indicative of a Domino name
			int slashIndex = value.indexOf('/');
			while(slashIndex > -1) {
				if(slashIndex == 0 || value.charAt(slashIndex-1) != '\\') {
					// Then it's probably a Domino name
					return value;
				}
				slashIndex = value.indexOf('/', slashIndex+1);
			}
			
			try {
				LdapName dn = new LdapName(value);
				StringBuilder result = new StringBuilder();
				// LdapName lists components in increasing-specificity order
				for(int i = dn.size()-1; i >= 0; i--) {
					if(result.length() > 0) {
						result.append("/"); //$NON-NLS-1$
					}
					
					String component = dn.get(i);
					// Domino likes the component name capitalized - probably not REQUIRED, but it shouldn't hurt
					int indexEq = component == null ? -1 : component.indexOf('=');
					if(component != null && indexEq > -1) {
						result.append(component.substring(0, indexEq).toUpperCase());
						result.append('=');
						result.append(component.substring(indexEq+1));
					} else {
						result.append(component);
					}
				}
				return result.toString();
			} catch(InvalidNameException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
