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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openntf.nsffile.config.NSFConfiguration;
import org.openntf.nsffile.ssh.auth.NotesPasswordAuthenticator;
import org.openntf.nsffile.ssh.auth.NotesPublicKeyAuthenticator;
import org.openntf.nsffile.ssh.scp.NSFScpFileOpener;
import org.openntf.nsffile.util.NotesThreadFactory;

import lombok.SneakyThrows;

/**
 * Manages the lifecycle of the SCP/SFTP server.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class SshListener implements ServletContextListener {
	public static final Logger log = Logger.getLogger(SshListener.class.getPackage().getName());
	static {
		log.setLevel(Level.ALL);
	}
	
	public static final String ENV_PORT = "SFTPNSFPort";
	public static final int DEFAULT_PORT = 9022;
	
	private SshServer server;

	@Inject @ConfigProperty(name=ENV_PORT, defaultValue=DEFAULT_PORT+"")
	private int port;
	
	@Inject private NSFConfiguration config;
	
	@Override
	@SneakyThrows
	public void contextInitialized(ServletContextEvent sce) {
		if(log.isLoggable(Level.INFO)) {
			log.info(getClass().getSimpleName() + " init");
		}
		
		String dataDir = NotesThreadFactory.call(session -> session.getEnvironmentString("Directory", true));
		Path keyPath = Paths.get(dataDir, getClass().getPackage().getName() + ".keys");

		String nsfPath = config.getNsfPath();
		if(log.isLoggable(Level.INFO)) {
			log.info(getClass().getSimpleName() + ": Using NSF path " + nsfPath);
			log.info(getClass().getSimpleName() + ": Using port " + port);
			log.info(getClass().getSimpleName() + ": Using key path " + keyPath);
		}
		
		server = SshServer.setUpDefaultServer();
		server.setPort(port);
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyPath));
		server.setPasswordAuthenticator(new NotesPasswordAuthenticator());
		server.setPublickeyAuthenticator(new NotesPublicKeyAuthenticator());
		server.setFileSystemFactory(session -> config.getFileSystem(session.getUsername()));
		
		SftpSubsystemFactory sftp = new SftpSubsystemFactory.Builder()
			.build();
		server.setSubsystemFactories(Collections.singletonList(sftp));
		
		ScpCommandFactory scp = new ScpCommandFactory.Builder()
			.withFileOpener(new NSFScpFileOpener(nsfPath))
			.build();
		server.setCommandFactory(scp);
		
		server.start();
	}
	
	@Override
	@SneakyThrows
	public void contextDestroyed(ServletContextEvent sce) {
		if(server != null) {
			server.close();
		}
	}
}
