package org.openntf.nsffile.ssh.auth;

import com.ibm.commons.util.StringUtil;

import lotus.domino.NotesException;
import lotus.domino.Session;

public abstract class AbstractNotesAuthenticator {

	/**
	 * Does some basic sanitizing on value to allow it to be inserted into a double-quoted string inside a Notes formula. 
	 */
	protected String escapeForFormulaString(String value) {
		if(StringUtil.isEmpty(value)) {
			return ""; //$NON-NLS-1$
		} else {
			return value.replace("\\", "\\\\").replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	protected String getItemValueStringForUser(Session session, String dominoName, String itemName) throws NotesException {
		if(StringUtil.isEmpty(dominoName)) {
			return ""; //$NON-NLS-1$
		} else {
			String lookupFormula = StringUtil.format(" @NameLookup([NoCache]:[Exhaustive]; \"{0}\"; \"{1}\") ", escapeForFormulaString(dominoName), escapeForFormulaString(itemName)); //$NON-NLS-1$
			// Don't worry too much if the user has more than one directory entry
			return StringUtil.toString(session.evaluate(lookupFormula).get(0));
		}
	}

}
