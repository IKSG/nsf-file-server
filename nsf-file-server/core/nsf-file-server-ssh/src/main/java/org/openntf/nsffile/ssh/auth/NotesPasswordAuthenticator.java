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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.NameNotFoundException;

import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;
import org.openntf.nsffile.core.util.NotesThreadFactory;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NotesPasswordAuthenticator implements PasswordAuthenticator {
	private static final Logger log = Logger.getLogger(NotesPasswordAuthenticator.class.getPackage().getName());

	@Override
	public boolean authenticate(String username, String password, ServerSession sshSession)
			throws PasswordChangeRequiredException, AsyncAuthException {
		return NotesThreadFactory.call(client -> {
			try {
				client.validateCredentials(null, username, password);
				return true;
			} catch(NameNotFoundException | AuthenticationException | AuthenticationNotSupportedException e) {
				return false;
			} catch(Throwable t) {
				if(log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Encountered exception validating Internet password", t);
				}
				throw t;
			}
		});
	}
}
