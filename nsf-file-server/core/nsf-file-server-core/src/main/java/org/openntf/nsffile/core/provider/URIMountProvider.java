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
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openntf.nsffile.core.spi.FileSystemMountProvider;

public class URIMountProvider implements FileSystemMountProvider {
	private static final Logger log = Logger.getLogger(URIMountProvider.class.getPackage().getName());

	@Override
	public String getName() {
		return "uri"; //$NON-NLS-1$
	}

	@Override
	public FileSystem createFileSystem(String dataSource, Map<String, Object> env) throws IOException {
		if(log.isLoggable(Level.FINEST)) {
			log.finest(MessageFormat.format("Building URI mount with dataSource={0} and env={1}", dataSource, env));
		}
		
		URI uri = URI.create(dataSource);
		try {
			try {
				return FileSystems.newFileSystem(uri, env);
			} catch(FileSystemAlreadyExistsException e) {
				// This can come up commonly with ZIP files
				return FileSystems.getFileSystem(uri);
			}
		} catch(Exception e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception building URI mount for dataSource \"{0}\" and environment \"{1}\"", dataSource, env), e);
			}
			throw e;
		}
	}

}
