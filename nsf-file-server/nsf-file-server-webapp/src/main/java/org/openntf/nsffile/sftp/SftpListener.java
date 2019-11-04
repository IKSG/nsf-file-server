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
package org.openntf.nsffile.sftp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.sshd.common.util.threads.NoCloseExecutor;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openntf.nsffile.util.NotesThreadFactory;

import lombok.SneakyThrows;
import lotus.domino.NotesFactory;
import lotus.domino.Session;

public class SftpListener implements ServletContextListener {
	public static final Logger log = Logger.getLogger(SftpListener.class.getPackage().getName());
	static {
		log.setLevel(Level.ALL);
	}
	
	public static final String ENV_DBPATH = "SFTPNSFPath";
	public static final String DEFAULT_DBPATH = "filestore.nsf";
	public static final String ENV_PORT = "SFTPNSFPort";
	public static final int DEFAULT_PORT = 9022;
	
	private SshServer server;

	@Inject @ConfigProperty(name=ENV_DBPATH, defaultValue=DEFAULT_DBPATH)
	private String nsfPath;
	@Inject @ConfigProperty(name=ENV_PORT, defaultValue=DEFAULT_PORT+"")
	private int port;
	
	@Override
	@SneakyThrows
	public void contextInitialized(ServletContextEvent sce) {
		if(log.isLoggable(Level.INFO)) {
			log.info(getClass().getSimpleName() + " init");
		}
		
		String dataDir = NotesThreadFactory.executor.submit(() -> {
			Session session = NotesFactory.createSession();
			try {
				return session.getEnvironmentString("Directory", true);
			} finally {
				session.recycle();
			}
		}).get();
		Path keyPath = Paths.get(dataDir, getClass().getPackage().getName() + ".keys");

		if(log.isLoggable(Level.INFO)) {
			log.info(getClass().getSimpleName() + ": Using NSF path " + nsfPath);
			log.info(getClass().getSimpleName() + ": Using port " + port);
			log.info(getClass().getSimpleName() + ": Using key path " + keyPath);
		}
		
		server = SshServer.setUpDefaultServer();
		server.setPort(port);
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyPath));
		server.setPasswordAuthenticator(new NotesPasswordAuthenticator());
		
		SftpSubsystemFactory sftp = new SftpSubsystemFactory.Builder()
			.withFileSystemAccessor(new NSFFileSystemAccessor(nsfPath))
			.withExecutorService(new NoCloseExecutor(NotesThreadFactory.executor))
			.build();
		server.setSubsystemFactories(Collections.singletonList(sftp));
		
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
