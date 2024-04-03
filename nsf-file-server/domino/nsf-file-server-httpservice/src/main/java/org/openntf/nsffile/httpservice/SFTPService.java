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
package org.openntf.nsffile.httpservice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import com.hcl.domino.DominoProcess;
import com.hcl.domino.commons.util.DominoUtils;
import com.ibm.designer.runtime.domino.adapter.ComponentModule;
import com.ibm.designer.runtime.domino.adapter.HttpService;
import com.ibm.designer.runtime.domino.adapter.LCDEnvironment;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletRequestAdapter;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletResponseAdapter;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpSessionAdapter;

import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.shell.UnknownCommandFactory;
import org.openntf.nsffile.core.config.DominoNSFConfiguration;
import org.openntf.nsffile.core.util.NSFFileUtil;
import org.openntf.nsffile.core.util.NotesThreadFactory;
import org.openntf.nsffile.ssh.CompositeNSFFileSystemFactory;
import org.openntf.nsffile.ssh.NSFHostKeyProvider;
import org.openntf.nsffile.ssh.SshServerAddin;
import org.openntf.nsffile.ssh.scp.CompositeScpFileOpener;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class SFTPService extends HttpService {
	private static final Logger log = Logger.getLogger(SFTPService.class.getPackage().getName());
	
	public static final int DEFAULT_PORT = 9022;
	
	private SshServerAddin server;
	private boolean enabled = true;

	public SFTPService(LCDEnvironment env) {
		super(env);

		DominoUtils.setJavaProperty("jnx.noinit", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		DominoUtils.setJavaProperty("jnx.noterm", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		DominoUtils.setJavaProperty("jnx.skipthreadwarning", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		DominoProcess.get().initializeProcess(new String[0]);
		
		this.enabled = DominoNSFConfiguration.instance.isEnabled();
		
		if(enabled) {
			// Kick off initialization on a separate thread to not block HTTP startup
			NotesThreadFactory.executor.submit(() -> {
				Path tempDir = null;
				if(OsUtils.isUNIX()) {
					try {
						tempDir = Files.createTempDirectory(Paths.get("/tmp"), getClass().getName()); //$NON-NLS-1$
						NSFFileUtil.setTempDirectory(tempDir);

					} catch(IOException e) {
						if(log.isLoggable(Level.SEVERE)) {
							log.log(Level.SEVERE, "Encountered unexpected IOException launching the SFTP server", e);
						}
						throw new UncheckedIOException(e);
					}
				}
				
				try {
					int port = DominoNSFConfiguration.instance.getPort();
					CompositeNSFFileSystemFactory fileSystemFactory = new CompositeNSFFileSystemFactory();
					ScpCommandFactory scp = new ScpCommandFactory.Builder()
						.withFileOpener(new CompositeScpFileOpener(fileSystemFactory))
						.withDelegate(new UnknownCommandFactory())
						.build();
					
					this.server = new SshServerAddin(port, new NSFHostKeyProvider(), fileSystemFactory, scp);
					
					server.start();
					server.join();
					
				} catch(Throwable t) {
					if(log.isLoggable(Level.SEVERE)) {
						log.log(Level.SEVERE, "Encountered exception initializing SFTP server", t);
					}
					server = null;
				} finally {
					if(tempDir != null) {
						try {
							NSFFileUtil.deltree(tempDir);
						} catch (IOException e) {
							if(log.isLoggable(Level.SEVERE)) {
								log.log(Level.SEVERE, "Encountered exception cleaning up temporary files", e);
							}
						}
					}
				}
			});
		}
	}
	
	@Override
	public void destroyService() {
		super.destroyService();
		
		NotesThreadFactory.term();
	}
	
	// *******************************************************************************
	// * Inapplicable methods
	// *******************************************************************************

	@Override
	public boolean doService(String contextPath, String path, HttpSessionAdapter httpSession, HttpServletRequestAdapter httpRequest,
			HttpServletResponseAdapter httpResponse) throws ServletException, IOException {
		// NOP
		return false;
	}

	@Override
	public void getModules(List<ComponentModule> modules) {
		// NOP
	}

}
