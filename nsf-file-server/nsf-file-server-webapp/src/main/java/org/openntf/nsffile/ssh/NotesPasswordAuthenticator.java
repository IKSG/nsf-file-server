/**
 * Copyright © 2019 Jesse Gallagher
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
package org.openntf.nsffile.ssh;

import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;
import org.openntf.nsffile.util.NotesThreadFactory;

import com.ibm.commons.util.StringUtil;

import lombok.SneakyThrows;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.Session;

public class NotesPasswordAuthenticator implements PasswordAuthenticator {

	@Override
	@SneakyThrows
	public boolean authenticate(String username, String password, ServerSession sshSession)
			throws PasswordChangeRequiredException, AsyncAuthException {
		return NotesThreadFactory.executor.submit(() -> {
			try {
				Session session = NotesFactory.createSession();
				try {
					String hashPassword = getHashPasswordForUser(session, username);
					if(StringUtil.isEmpty(hashPassword)) {
						return false;
					} else {
						String verifyFormula = StringUtil.format(" @VerifyPassword(\"{0}\"; \"{1}\") ", escapeForFormulaString(password), hashPassword); //$NON-NLS-1$
						Object result = session.evaluate(verifyFormula).get(0);
						if(Double.valueOf(1).equals(result)) {
							return true;
						}
					}
				} finally {
					session.recycle();
				}
			} catch(NotesException e) {
			}
			return false;
		}).get();
	}
	
	/**
	 * Looks up the HTTPPassword value for the provided Domino-format user name.
	 */
	private String getHashPasswordForUser(Session session, String dominoName) throws NotesException {
		if(StringUtil.isEmpty(dominoName)) {
			return ""; //$NON-NLS-1$
		} else {
			String lookupFormula = StringUtil.format(" @NameLookup([NoCache]:[Exhaustive]; \"{0}\"; \"HTTPPassword\") ", escapeForFormulaString(dominoName)); //$NON-NLS-1$
			// Don't worry too much if the user has more than one directory entry
			String hashPassword = StringUtil.toString(session.evaluate(lookupFormula).get(0));
			
			return hashPassword;
		}
	}
	
	/**
	 * Does some basic sanitizing on value to allow it to be inserted into a double-quoted string inside a Notes formula. 
	 */
	private String escapeForFormulaString(String value) {
		if(StringUtil.isEmpty(value)) {
			return ""; //$NON-NLS-1$
		} else {
			return value.replace("\\", "\\\\").replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

}
