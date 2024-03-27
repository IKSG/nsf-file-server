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
package org.openntf.nsffile.ssh.auth;

import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;
import org.openntf.nsffile.core.util.NotesThreadFactory;

import com.ibm.commons.util.StringUtil;

import lombok.SneakyThrows;
import lotus.domino.NotesException;
import lotus.domino.Session;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NotesPasswordAuthenticator extends AbstractNotesAuthenticator implements PasswordAuthenticator {

	@Override
	@SneakyThrows
	public boolean authenticate(String username, String password, ServerSession sshSession)
			throws PasswordChangeRequiredException, AsyncAuthException {
		return NotesThreadFactory.call(session -> {
			String hashPassword = getHashPasswordForUser(session, username);
			if(StringUtil.isEmpty(hashPassword)) {
				return false;
			} else {
				// TODO switch to NABLookupBasicAuthentication
				String verifyFormula = StringUtil.format(" @VerifyPassword(\"{0}\"; \"{1}\") ", escapeForFormulaString(password), hashPassword); //$NON-NLS-1$
				Object result = session.evaluate(verifyFormula).get(0);
				if(Double.valueOf(1).equals(result)) {
					return true;
				}
			}
			return false;
		});
	}
	
	/**
	 * Looks up the HTTPPassword value for the provided Domino-format user name.
	 */
	private String getHashPasswordForUser(Session session, String dominoName) throws NotesException {
		return getItemValueStringForUser(session, dominoName, "HTTPPassword"); //$NON-NLS-1$
	}

}
