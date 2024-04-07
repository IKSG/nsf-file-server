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
package org.openntf.nsffile.core.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CompositeDirectoryStream implements DirectoryStream<Path> {
	private final List<Path> paths;
	
	public CompositeDirectoryStream(CompositeFileSystem fileSystem) {
		paths = fileSystem.getFileSystems().keySet()
			.stream()
			.map(mount -> fileSystem.getPath("/", mount)) //$NON-NLS-1$
			.collect(Collectors.toList());
	}

	@Override
	public Iterator<Path> iterator() {
		return paths.iterator();
	}

	@Override
	public void close() throws IOException {
		
	}

}
