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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.text.MessageFormat;
import java.util.EnumSet;

import com.hcl.domino.DominoClient;
import com.hcl.domino.data.Database;
import com.hcl.domino.data.Document;
import com.hcl.domino.data.Document.EncryptionMode;
import com.hcl.domino.data.DominoCollection;
import com.hcl.domino.data.Item.ItemFlag;
import com.ibm.commons.util.StringUtil;

import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.openntf.nsffile.core.config.DominoNSFConfiguration;
import org.openntf.nsffile.core.util.NotesThreadFactory;

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
		return NotesThreadFactory.call(client -> {
			Document keyPairDoc = getKeyPairDocument(client, false);
			if(keyPairDoc == null) {
				return null;
			}
			
			// The full pair is in PrivateKey
			String privateKey = keyPairDoc.get(DominoNSFConfiguration.ITEM_PRIVATEKEY, String.class, null);
			if(StringUtil.isEmpty(privateKey)) {
				return null;
			}
			
			byte[] privateKeyData = privateKey.getBytes(StandardCharsets.UTF_8);
			try(ByteArrayInputStream is = new ByteArrayInputStream(privateKeyData)) {
				return SecurityUtils.loadKeyPairIdentities(session, () -> "NSF", is, null); //$NON-NLS-1$
			}
		});
	}
	
	@Override
	protected void writeKeyPair(KeyPair kp, Path keyPath) throws IOException, GeneralSecurityException {
		// Also write it to the NSF to see how that shakes out
		NotesThreadFactory.run(client -> {
			Document keyPairDoc = getKeyPairDocument(client, true);
			
			OpenSSHKeyPairResourceWriter writer = new OpenSSHKeyPairResourceWriter();
			
			byte[] data;
	        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
	            writer.writePrivateKey(kp, "host key", null, out); //$NON-NLS-1$
	            out.flush();
	            data = out.toByteArray();
	        }
	        
	        keyPairDoc.replaceItemValue(DominoNSFConfiguration.ITEM_PRIVATEKEY, EnumSet.of(ItemFlag.SEALED), new String(data, StandardCharsets.UTF_8));
	        
	        // Base64-encoded data with human-readable label 
	        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
	        	writer.writePublicKey(kp, "public key", out); //$NON-NLS-1$
	        	out.flush();
	        	data = out.toByteArray();
	        }
	        
	        keyPairDoc.replaceItemValue(DominoNSFConfiguration.ITEM_PUBKEY, new String(data, StandardCharsets.UTF_8));

	        keyPairDoc.computeWithForm(true, null);
	        keyPairDoc.sign();
	        keyPairDoc = keyPairDoc.copyAndEncrypt(null, EnumSet.of(EncryptionMode.ENCRYPT_WITH_USER_PUBLIC_KEY));
	        keyPairDoc.save();
		});
	}
	
	private Document getKeyPairDocument(DominoClient client, boolean create) {
		Database configDb = client.openDatabase(DominoNSFConfiguration.instance.getConfigNsfPath());
		DominoCollection keyPairs = configDb.openCollection(DominoNSFConfiguration.VIEW_SSHKEYPAIRS)
			.orElseThrow(() -> new IllegalStateException(MessageFormat.format("Unable to open view \"{0}\" in database \"{1}\"", DominoNSFConfiguration.VIEW_SSHKEYPAIRS, DominoNSFConfiguration.instance.getConfigNsfPath())));;
		return keyPairs.query()
			.selectByKey(client.getIDUserName(), true)
			.firstId()
			.flatMap(configDb::getDocumentById)
			.orElseGet(() -> {
				Document keyPairDoc = configDb.createDocument();
				keyPairDoc.replaceItemValue("Form", DominoNSFConfiguration.FORM_SSHKEYPAIR); //$NON-NLS-1$
				keyPairDoc.replaceItemValue(DominoNSFConfiguration.ITEM_SERVERNAME, client.getIDUserName());
				return keyPairDoc;
			});
	}
}
