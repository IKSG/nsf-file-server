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
package org.openntf.nsffile.config;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.fs.util.NSFPathUtil;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
@ApplicationScoped
public class NSFConfiguration implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String ENV_DBPATH = "SFTPNSFPath";
	public static final String DEFAULT_DBPATH = "filestore.nsf";
	
	@Inject @ConfigProperty(name=ENV_DBPATH, defaultValue=DEFAULT_DBPATH)
	String nsfPath;
	
	public String getNsfPath() {
		return nsfPath;
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
