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
package org.openntf.nsffile.ssh.provider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.sshd.common.file.root.RootedFileSystemProvider;
import org.openntf.nsffile.commons.spi.FileSystemMountProvider;

public class URIMountProvider implements FileSystemMountProvider {

	@Override
	public String getName() {
		return "uri"; //$NON-NLS-1$
	}

	@Override
	public FileSystem createFileSystem(String dataSource, Map<String, Object> env) throws IOException {
		RootedFileSystemProvider provider = new RootedFileSystemProvider();
		URI uri = URI.create(dataSource);
		Path path = Paths.get(uri);
		return provider.newFileSystem(path, env);
	}

}
