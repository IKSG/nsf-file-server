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
package org.openntf.nsffile.ssh.scp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Set;

import org.apache.sshd.common.scp.ScpTimestamp;
import org.apache.sshd.common.scp.helpers.DefaultScpFileOpener;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.fs.util.NSFPathUtil;

import com.ibm.commons.util.StringUtil;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NSFScpFileOpener extends DefaultScpFileOpener {
	
	private final String nsfPath;
	
	public NSFScpFileOpener(String nsfPath) {
		super();
		this.nsfPath = nsfPath;
	}

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
			return super.resolveIncomingReceiveLocation(session, nsfPath, recursive, shouldBeDir, preserve);
		} catch(URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public Path resolveLocalPath(org.apache.sshd.common.session.Session session, FileSystem fileSystem,
			String commandPath) throws IOException, InvalidPathException {
		if(".".equals(commandPath) || StringUtil.isEmpty(commandPath)) { //$NON-NLS-1$
			return fileSystem.getPath("/"); //$NON-NLS-1$
		} else {
			return fileSystem.getPath(commandPath);
		}
	}
}