package org.openntf.nsffile.ssh.auth;

import java.security.PublicKey;
import java.util.Base64;

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.apache.sshd.common.util.buffer.keys.BufferPublicKeyParser;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.openntf.nsffile.util.NotesThreadFactory;

import com.ibm.commons.util.StringUtil;

import lombok.SneakyThrows;
import lotus.domino.NotesFactory;
import lotus.domino.Session;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NotesPublicKeyAuthenticator extends AbstractNotesAuthenticator implements PublickeyAuthenticator {
	
	public static final String ITEM_PUBKEY = "sshPublicKey";

	@Override
	@SneakyThrows
	public boolean authenticate(String username, PublicKey key, ServerSession serverSession) throws AsyncAuthException {
		return NotesThreadFactory.executor.submit(() -> {
			try {
				Session session = NotesFactory.createSession();
				try {
					String publicKey = getItemValueStringForUser(session, username, ITEM_PUBKEY);
					if(StringUtil.isEmpty(publicKey)) {
						return false;
					} else {
						int space = publicKey.indexOf(' ');
						String type = publicKey.substring(0, space);
						int lastSpace = publicKey.lastIndexOf(' ');
						String encKey = publicKey.substring(space+1, lastSpace);
						byte[] keyBytes = Base64.getDecoder().decode(encKey);
						Buffer keyBuf = new ByteArrayBuffer(keyBytes);
						// The first bit is "ssh-rsa" - discard
						keyBuf.getString();
						PublicKey dirKey = BufferPublicKeyParser.DEFAULT.getRawPublicKey(type, keyBuf);
						return key.equals(dirKey);
					}
				} finally {
					session.recycle();
				}
			} catch(Throwable e) {
				e.printStackTrace();
			}
			return false;
		}).get();
	}

}
