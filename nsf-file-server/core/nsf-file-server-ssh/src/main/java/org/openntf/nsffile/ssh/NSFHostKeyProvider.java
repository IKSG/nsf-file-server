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
package org.openntf.nsffile.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Base64;

import com.ibm.commons.util.StringUtil;

import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.openntf.nsffile.core.config.DominoNSFConfiguration;
import org.openntf.nsffile.core.util.NSFFileUtil;
import org.openntf.nsffile.core.util.NotesThreadFactory;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;

/**
 * This subclass of {@link AbstractGeneratorHostKeyProvider} overrides a few methods
 * to piggyback on the key-generation code on that class while redirecting reading
 * and writing to an NSF.
 */
public class NSFHostKeyProvider extends SimpleGeneratorHostKeyProvider {

	public NSFHostKeyProvider() {
		// The parent only checks the nullness of this value, not whether it's
		//   an actual file until it comes to the writing
		super(Paths.get(".")); //$NON-NLS-1$
	}
	
	@Override
	protected Iterable<KeyPair> loadFromFile(SessionContext session, String alg, Path keyPath)
			throws IOException, GeneralSecurityException {
		return NotesThreadFactory.call(dominoSession -> {
			Document keyPairDoc = getKeyPairDocument(dominoSession, false);
			if(keyPairDoc == null) {
				return null;
			}
			
			// The full pair is in PrivateKey
			String privateKeyB64 = keyPairDoc.getItemValueString(DominoNSFConfiguration.ITEM_PRIVATEKEY);
			if(StringUtil.isEmpty(privateKeyB64)) {
				return null;
			}
			
			byte[] privateKeyData = Base64.getMimeDecoder().decode(privateKeyB64);
			try(ByteArrayInputStream is = new ByteArrayInputStream(privateKeyData)) {
				return SecurityUtils.loadKeyPairIdentities(session, () -> "NSF", is, null); //$NON-NLS-1$
			}
		});
	}
	
	@Override
	protected void writeKeyPair(KeyPair kp, Path keyPath) throws IOException, GeneralSecurityException {
		// Also write it to the NSF to see how that shakes out
		NotesThreadFactory.run(dominoSession -> {
			Document keyPairDoc = getKeyPairDocument(dominoSession, true);
			
			OpenSSHKeyPairResourceWriter writer = new OpenSSHKeyPairResourceWriter();
			
			byte[] data;
	        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
	            writer.writePrivateKey(kp, "host key", null, out); //$NON-NLS-1$
	            out.flush();
	            data = out.toByteArray();
	        }
	        
	        keyPairDoc.replaceItemValue(DominoNSFConfiguration.ITEM_PRIVATEKEY, Base64.getMimeEncoder().encodeToString(data)).setEncrypted(true);
	        
	        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
	        	writer.writePublicKey(kp, "public key", out); //$NON-NLS-1$
	        	out.flush();
	        	data = out.toByteArray();
	        }
	        
	        keyPairDoc.replaceItemValue(DominoNSFConfiguration.ITEM_PUBKEY, Base64.getMimeEncoder().encodeToString(data)).setSummary(false);
	        
	        keyPairDoc.encrypt();
	        keyPairDoc.computeWithForm(false, false);
	        keyPairDoc.save();
		});
	}
	
	private Document getKeyPairDocument(Session dominoSession, boolean create) throws NotesException {
		Database configDb = NSFFileUtil.openDatabase(dominoSession, DominoNSFConfiguration.instance.getConfigNsfPath());
		View keyPairs = configDb.getView(DominoNSFConfiguration.VIEW_SSHKEYPAIRS);
		Document keyPairDoc = keyPairs.getDocumentByKey(dominoSession.getUserName(), true);
		if(keyPairDoc == null && create) {
			keyPairDoc = configDb.createDocument();
			keyPairDoc.replaceItemValue("Form", DominoNSFConfiguration.FORM_SSHKEYPAIR); //$NON-NLS-1$
			keyPairDoc.replaceItemValue(DominoNSFConfiguration.ITEM_SERVERNAME, dominoSession.getUserName());
		}
		return keyPairDoc;
	}
}
