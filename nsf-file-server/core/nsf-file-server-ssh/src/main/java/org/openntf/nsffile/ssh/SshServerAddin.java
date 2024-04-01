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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hcl.domino.DominoClient;
import com.hcl.domino.mq.MessageQueue;
import com.hcl.domino.server.RunJavaAddin;
import com.hcl.domino.server.ServerStatusLine;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.ServerBuilder;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.RejectAllPasswordAuthenticator;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.openntf.nsffile.core.config.DominoNSFConfiguration;
import org.openntf.nsffile.ssh.auth.NotesPasswordAuthenticator;
import org.openntf.nsffile.ssh.auth.NotesPublicKeyAuthenticator;

/**
 * Frontend-independent manager for running the SSH/SFTP server.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
@SuppressWarnings("nls")
public class SshServerAddin extends RunJavaAddin {
	public static final Logger log = Logger.getLogger(SshServerAddin.class.getPackage().getName());
	
	public static final String ADDIN_NAME = "SFTP Server";
	public static final String QUEUE_NAME = "sftp";
	
	private final int port;
	private final KeyPairProvider keyPairProvider;
	private final FileSystemFactory fileSystemFactory;
	private final ScpCommandFactory scpCommandFactory;

	public SshServerAddin(int port, KeyPairProvider keyPairProvider, FileSystemFactory fileSystemFactory, ScpCommandFactory scpCommandFactory) {
		super(ADDIN_NAME, QUEUE_NAME);
		
		this.port = port;
		this.keyPairProvider = keyPairProvider;
		this.fileSystemFactory = fileSystemFactory;
		this.scpCommandFactory = scpCommandFactory;
	}
	
	@Override
	protected void runAddin(DominoClient client, ServerStatusLine statusLine, MessageQueue mq) {
		if(log.isLoggable(Level.INFO)) {
			log.info(getClass().getSimpleName() + ": Startup");
			log.info(getClass().getSimpleName() + ": Using port " + port);
			log.info(getClass().getSimpleName() + ": Using key provider " + keyPairProvider);
		}
		
		ServerBuilder builder = ServerBuilder.builder()
			.fileSystemFactory(fileSystemFactory)
			.publickeyAuthenticator(new NotesPublicKeyAuthenticator());
		try(SshServer server = builder.build()) {
			server.setPort(port);
			server.setKeyPairProvider(keyPairProvider);
			
			if(DominoNSFConfiguration.instance.isAllowPasswordAuth()) {
				server.setPasswordAuthenticator(new NotesPasswordAuthenticator());
			} else {
				server.setPasswordAuthenticator(RejectAllPasswordAuthenticator.INSTANCE);
			}
			
			SftpSubsystemFactory sftp = new SftpSubsystemFactory.Builder()
				.build();
			server.setSubsystemFactories(Collections.singletonList(sftp));
			
			server.setCommandFactory(scpCommandFactory);
			
			server.start();
			
			statusLine.setLine(MessageFormat.format("Listen for connect requests on TCP Port:{0}", Integer.toString(port)));

			if(log.isLoggable(Level.INFO)) {
				log.info(MessageFormat.format("Initialized SFTP server on port {0}", Integer.toString(port)));
			}
			System.out.println(MessageFormat.format("Initialized SFTP server on port {0}", Integer.toString(port)));
			
			while (!mq.isQuitPending()) {
	          Optional<String> message;
	          while ((message = mq.get(5, TimeUnit.SECONDS)) != null) {
	            message.ifPresent(msg -> {
	              // if we got a message in the queue, process it
	              // TODO actually implement some messages
	            });
	          }
	        }
		} catch(IOException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, "Encountered exception running SFTP server", e);
			}
		} catch (InterruptedException e) {
			// Fine - meant to quit
		}
		
		System.out.println("SFTP Server: Shutdown");
	}
}
