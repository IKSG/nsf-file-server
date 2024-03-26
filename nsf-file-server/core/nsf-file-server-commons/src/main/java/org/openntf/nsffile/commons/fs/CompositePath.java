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
package org.openntf.nsffile.commons.fs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

import org.apache.sshd.common.file.util.BasePath;

public class CompositePath extends BasePath<CompositePath, CompositeFileSystem> {
	
	public CompositePath(CompositeFileSystem fileSystem, String root, List<String> names) {
		super(fileSystem, root, names);
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		// TODO: handle links
		CompositePath absolute = toAbsolutePath();
        FileSystem fs = getFileSystem();
        FileSystemProvider provider = fs.provider();
        provider.checkAccess(absolute);
        return absolute;
	}


}
