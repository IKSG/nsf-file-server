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
package org.openntf.nsffile.ssh.scp;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.ibm.commons.util.StringUtil;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.scp.common.helpers.DefaultScpFileOpener;

public class CompositeScpFileOpener extends DefaultScpFileOpener {
	private final FileSystemFactory fileSystemFactory;

	public CompositeScpFileOpener(FileSystemFactory fileSystemFactory) {
		this.fileSystemFactory = fileSystemFactory;
	}

	@Override
	public Path resolveIncomingReceiveLocation(org.apache.sshd.common.session.Session session, Path path,
			boolean recursive, boolean shouldBeDir, boolean preserve) throws IOException {
		FileSystem fs = fileSystemFactory.createFileSystem(session);
		Path nsfPath = fs.getPath(path.toString());
		return super.resolveIncomingReceiveLocation(session, nsfPath, recursive, shouldBeDir, preserve);
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
