/**
 * Copyright © 2019-2020 Jesse Gallagher
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
package org.openntf.nsffile.listener;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openntf.nsffile.config.NSFConfiguration;
import org.openntf.nsffile.ssh.SshServerDelegate;
import org.openntf.nsffile.util.NotesThreadFactory;

import lombok.SneakyThrows;

/**
 * Manages the lifecycle of the SCP/SFTP server.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
@SuppressWarnings("nls")
public class SshListener implements ServletContextListener {
	public static final Logger log = Logger.getLogger(SshListener.class.getPackage().getName());
	
	public static final String ENV_PORT = "SFTPNSFPort"; //$NON-NLS-1$
	public static final int DEFAULT_PORT = 9022;
	
	private SshServerDelegate server;

	@Inject @ConfigProperty(name=ENV_PORT, defaultValue=DEFAULT_PORT+"")
	private int port;
	
	@Inject private NSFConfiguration config;
	
	@Override
	@SneakyThrows
	public void contextInitialized(ServletContextEvent sce) {
		if(log.isLoggable(Level.INFO)) {
			log.info(getClass().getSimpleName() + " init");
		}
		
		String dataDir = NotesThreadFactory.call(session -> session.getEnvironmentString("Directory", true)); //$NON-NLS-1$
		Path keyPath = Paths.get(dataDir, getClass().getPackage().getName() + ".keys"); //$NON-NLS-1$

		String nsfPath = config.getNsfPath();
		this.server = new SshServerDelegate(nsfPath, port, keyPath, session -> config.getFileSystem(session.getUsername()));
		
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
