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
package org.openntf.nsffile.domino.config;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.util.Collections;

import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.fs.util.NSFPathUtil;

import com.ibm.commons.util.StringUtil;
import com.ibm.domino.napi.NException;
import com.ibm.domino.napi.c.Os;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public enum DominoNSFConfiguration {
	instance;
	
	public static final String ENV_DBPATH = "SFTPNSFPath"; //$NON-NLS-1$
	public static final String DEFAULT_DBPATH = "filestore.nsf"; //$NON-NLS-1$
	public static final String ENV_PORT = "SFTPNSFPort"; //$NON-NLS-1$
	public static final int DEFAULT_PORT = 9022;

	private final String nsfPath;
	private final int port;
	
	private DominoNSFConfiguration() {
		try {
			String envProperty = Os.OSGetEnvironmentString(ENV_DBPATH);
			if(StringUtil.isEmpty(envProperty)) {
				envProperty = DEFAULT_DBPATH;
			}
			this.nsfPath = envProperty;
			
			String envPort = Os.OSGetEnvironmentString(ENV_PORT);
			int port;
			if(StringUtil.isEmpty(envPort)) {
				port = DEFAULT_PORT;
			} else {
				port = Integer.valueOf(envPort);
			}
			this.port = port;
		} catch(NException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getNsfPath() {
		return nsfPath;
	}
	
	public int getPort() {
		return port;
	}
	
	public FileSystem getFileSystem(String username) throws IOException {
		try {
			URI uri = NSFPathUtil.toFileSystemURI(username, nsfPath);
			return NSFFileSystemProvider.instance.getOrCreateFileSystem(uri, Collections.emptyMap());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
