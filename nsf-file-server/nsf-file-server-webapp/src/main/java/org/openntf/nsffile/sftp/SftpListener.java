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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.sshd.common.scp.ScpTimestamp;
import org.apache.sshd.common.scp.helpers.DefaultScpFileOpener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.util.NSFPathUtil;
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
		server.setFileSystemFactory(session -> {
			try {
				URI uri = NSFPathUtil.toFileSystemURI(session.getUsername(), nsfPath);
				return NSFFileSystemProvider.instance.getOrCreateFileSystem(uri, Collections.emptyMap());
			} catch (URISyntaxException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});
		
		SftpSubsystemFactory sftp = new SftpSubsystemFactory.Builder()
			.build();
		server.setSubsystemFactories(Collections.singletonList(sftp));
		
		ScpCommandFactory scp = new ScpCommandFactory.Builder()
			.withFileOpener(new DefaultScpFileOpener() {
				@Override
				public Path resolveIncomingFilePath(org.apache.sshd.common.session.Session session, Path localPath,
						String name, boolean preserve, Set<PosixFilePermission> permissions, ScpTimestamp time)
						throws IOException {
					return super.resolveIncomingFilePath(session, localPath, name, preserve, permissions, time);
				}
				
				@Override
				public Path resolveIncomingReceiveLocation(org.apache.sshd.common.session.Session session, Path path,
						boolean recursive, boolean shouldBeDir, boolean preserve) throws IOException {
					try {
						URI uri = NSFPathUtil.toFileSystemURI(session.getUsername(), nsfPath);
						FileSystem fs = NSFFileSystemProvider.instance.getOrCreateFileSystem(uri, Collections.emptyMap());
						Path nsfPath = fs.getPath(path.toString());
						Path result = super.resolveIncomingReceiveLocation(session, nsfPath, recursive, shouldBeDir, preserve);
						return result;
					} catch(URISyntaxException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
				
				@Override
				public Path resolveLocalPath(org.apache.sshd.common.session.Session session, FileSystem fileSystem,
						String commandPath) throws IOException, InvalidPathException {
					if(".".equals(commandPath)) {
						return fileSystem.getPath("/");
					} else {
						return fileSystem.getPath(commandPath);
					}
				}
			})
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
