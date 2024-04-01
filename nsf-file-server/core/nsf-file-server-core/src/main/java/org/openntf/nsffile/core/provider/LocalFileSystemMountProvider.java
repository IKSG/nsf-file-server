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
package org.openntf.nsffile.core.provider;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.sshd.common.file.root.RootedFileSystemProvider;
import org.openntf.nsffile.core.spi.FileSystemMountProvider;

public class LocalFileSystemMountProvider implements FileSystemMountProvider {
	private final RootedFileSystemProvider provider = new RootedFileSystemProvider();

	@Override
	public String getName() {
		return "filesystem"; //$NON-NLS-1$
	}

	@Override
	public FileSystem createFileSystem(String dataSource, Map<String, Object> env) throws IOException {
		Path path = Paths.get(dataSource);
		
		try {
			return provider.newFileSystem(path, env);
		} catch(FileSystemAlreadyExistsException e) {
			return provider.getFileSystem(path.toUri());
		}
	}

}
