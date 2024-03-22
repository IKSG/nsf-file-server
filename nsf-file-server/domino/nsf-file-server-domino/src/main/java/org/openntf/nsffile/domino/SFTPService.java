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
package org.openntf.nsffile.domino;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.openntf.nsffile.domino.config.DominoNSFConfiguration;
import org.openntf.nsffile.ssh.SshServerDelegate;
import org.openntf.nsffile.util.NotesThreadFactory;

import com.ibm.designer.runtime.domino.adapter.ComponentModule;
import com.ibm.designer.runtime.domino.adapter.HttpService;
import com.ibm.designer.runtime.domino.adapter.LCDEnvironment;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletRequestAdapter;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletResponseAdapter;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpSessionAdapter;
import com.ibm.domino.napi.NException;
import com.ibm.domino.napi.c.Os;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class SFTPService extends HttpService {
	private static final Logger log = Logger.getLogger(SFTPService.class.getPackage().getName());
	
	public static final String ENV_ENABLE = "SFTPNSFEnable"; //$NON-NLS-1$
	public static final String ENV_PORT = "SFTPNSFPort"; //$NON-NLS-1$
	public static final int DEFAULT_PORT = 9022;
	
	private SshServerDelegate server;
	private boolean enabled = true;

	public SFTPService(LCDEnvironment env) {
		super(env);
		
		try {
			String envEnable = Os.OSGetEnvironmentString(ENV_ENABLE);
			enabled = !"0".equals(envEnable); //$NON-NLS-1$
		} catch (NException e) {
			if(log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, MessageFormat.format("Encountered exception retrieving INI parameter {0}", ENV_ENABLE), e);
			}
		}
		
		if(enabled) {
			// Kick off initialization on a separate thread to not block HTTP startup
			NotesThreadFactory.executor.submit(() -> {
				try {
					String dataDir = Os.OSGetEnvironmentString("Directory"); //$NON-NLS-1$
					Path keyPath = Paths.get(dataDir, getClass().getPackage().getName() + ".keys"); //$NON-NLS-1$
			
					String nsfPath = DominoNSFConfiguration.instance.getNsfPath();
					int port = DominoNSFConfiguration.instance.getPort();
					this.server = new SshServerDelegate(nsfPath, port, keyPath, session -> DominoNSFConfiguration.instance.getFileSystem(session.getUsername()));
					
					server.start();
				} catch(Throwable t) {
					if(log.isLoggable(Level.SEVERE)) {
						log.log(Level.SEVERE, "Encountered exception initializing SFTP server", t);
					}
					server = null;
				}
			});
		}
	}
	
	@Override
	public void destroyService() {
		super.destroyService();
		
		if(enabled) {
			if(server != null) {
				try {
					server.close();
				} catch (IOException e) {
					if(log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "Encountered exception closing SFTP server", e);
					}
				}
			}
			
			NotesThreadFactory.term();
		}
	}
	
	// *******************************************************************************
	// * Inapplicable methods
	// *******************************************************************************

	@Override
	public boolean doService(String arg0, String arg1, HttpSessionAdapter arg2, HttpServletRequestAdapter arg3,
			HttpServletResponseAdapter arg4) throws ServletException, IOException {
		// NOP
		return false;
	}

	@Override
	public void getModules(List<ComponentModule> modules) {
		// NOP
	}

}
