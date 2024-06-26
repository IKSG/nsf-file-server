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
package org.openntf.nsffile.fs.abstractnsf;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NSFDirectoryStream implements DirectoryStream<Path> {
	private static final Logger log = Logger.getLogger(NSFDirectoryStream.class.getPackage().getName());
	
	private final List<Path> paths;

	public NSFDirectoryStream(AbstractNSFFileSystemProvider provider, NSFPath dir) {
		try {
			this.paths = provider.getAccessor().getDirectoryEntries(dir).parallelStream()
				.map(name -> dir.resolve(name))
				.collect(Collectors.toList());
		} catch(Exception e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception constructing directory stream for provider {0}, dir {1}", provider, dir), e);
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public Iterator<Path> iterator() {
		return paths.iterator();
	}

	@Override
	public void close() throws IOException {
	}
}
