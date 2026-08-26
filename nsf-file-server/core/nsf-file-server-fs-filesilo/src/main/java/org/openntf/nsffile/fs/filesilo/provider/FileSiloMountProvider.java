/**
 * Copyright (c) 2019-2026 Jesse Gallagher
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
package org.openntf.nsffile.fs.filesilo.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import com.ibm.commons.util.StringUtil;

import org.openntf.nsffile.core.util.AbstractFileSystemMountProvider;
import org.openntf.nsffile.core.util.NSFFileUtil;
import org.openntf.nsffile.fs.filesilo.FileSiloFileSystemProvider;

public class FileSiloMountProvider extends AbstractFileSystemMountProvider {
	public FileSiloMountProvider() {
		super(FileSiloFileSystemProvider.SCHEME);
	}

	@Override
	public FileSystem createFileSystem(String dataSource, Map<String, Object> env) throws IOException {
		String username = (String)env.get(KEY_USERNAME);
		if(StringUtil.isEmpty(username)) {
			throw new IllegalArgumentException(MessageFormat.format("env map must contain a {0} key", KEY_USERNAME));
		}
		try {
			URI uri = NSFFileUtil.toFileSystemURI(getName(), username, dataSource);
			return FileSiloFileSystemProvider.instance.getOrCreateFileSystem(uri, Collections.emptyMap());
		} catch (URISyntaxException e) {
			throw new IOException(MessageFormat.format("Unable to build URI for data source {0}", dataSource), e);
		}
	}

}
