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

import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hcl.domino.DominoClient;
import com.ibm.commons.util.StringUtil;

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.apache.sshd.common.util.buffer.keys.BufferPublicKeyParser;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.openntf.nsffile.core.util.NSFFileUtil;
import org.openntf.nsffile.core.util.NotesThreadFactory;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NotesPublicKeyAuthenticator implements PublickeyAuthenticator {
	private static final Logger log = Logger.getLogger(NotesPublicKeyAuthenticator.class.getPackage().getName());
	
	public static final String ITEM_PUBKEY = "sshPublicKey"; //$NON-NLS-1$

	@Override
	public boolean authenticate(String username, PublicKey key, ServerSession serverSession) throws AsyncAuthException {
		return NotesThreadFactory.call(client -> {
			List<String> publicKeys = getItemValueStringListForUser(client, username, ITEM_PUBKEY);
			if(publicKeys.isEmpty() || (publicKeys.size() == 1 && StringUtil.isEmpty(publicKeys.get(0)))) {
				return false;
			} else {
				for(String publicKey : publicKeys) {
					try {
						int space = publicKey.indexOf(' ');
						String type = publicKey.substring(0, space);
						int lastSpace = publicKey.lastIndexOf(' ');
						if(lastSpace == space) {
							// Then there's no trailing user/machine note
							lastSpace = publicKey.length();
						}
						String encKey = publicKey.substring(space+1, lastSpace);
						byte[] keyBytes = Base64.getDecoder().decode(encKey);
						Buffer keyBuf = new ByteArrayBuffer(keyBytes);
						// The first bit is "ssh-rsa" - discard
						keyBuf.getString();
						PublicKey dirKey = BufferPublicKeyParser.DEFAULT.getRawPublicKey(type, keyBuf);
						if(key.equals(dirKey)) {
							return true;
						}
					} catch(Exception e) {
						// Log and move on
						if(log.isLoggable(Level.WARNING)) {
							log.log(Level.WARNING, MessageFormat.format("Encountered exception parsing SSH public key {0}", publicKey), e);
						}
					}
				}
				return false;
			}
		});
	}
	
	protected List<String> getItemValueStringListForUser(DominoClient client, String dominoName, String itemName) {
		if(StringUtil.isEmpty(dominoName)) {
			return Collections.emptyList();
		} else {
			return client.openUserDirectory(null)
				.lookupUserValue(dominoName, itemName)
				.map(result -> result.get(itemName))
				.map(NSFFileUtil::toStringList)
				.orElseGet(Collections::emptyList);
		}
	}

}
