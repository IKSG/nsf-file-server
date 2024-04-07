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
package org.openntf.nsffile.fs.nsfwebcontent;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.ibm.commons.util.StringUtil;

import org.openntf.nsffile.fs.abstractnsf.AbstractNSFFileSystemProvider;
import org.openntf.nsffile.fs.abstractnsf.NSFFileSystem;
import org.openntf.nsffile.fs.nsfwebcontent.db.WebContentNSFAccessor;
import org.openntf.nsffile.fs.nsfwebcontent.util.WebContentPathUtil;

/**
 * Java NIO Filesystem implementation for NSF file storage.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class WebContentFileSystemProvider extends AbstractNSFFileSystemProvider {
	public static final String SCHEME = "nsfwebcontent"; //$NON-NLS-1$
	public static final Logger log = Logger.getLogger(WebContentFileSystemProvider.class.getPackage().getName());
	
	public static final WebContentFileSystemProvider instance = new WebContentFileSystemProvider();
	
	private Map<String, FileSystem> fileSystems = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	
	public WebContentFileSystemProvider() {
		super(WebContentNSFAccessor.instance);
	}

	// *******************************************************************************
	// * Filesystem Operations
	// *******************************************************************************
	
	@Override
	public String getScheme() {
		return SCHEME;
	}
	
	public FileSystem getOrCreateFileSystem(URI uri, Map<String, ?> env) throws IOException {
		Objects.requireNonNull(uri, "uri cannot be null"); //$NON-NLS-1$
		
		String nsfPath = WebContentPathUtil.extractApiPath(uri);
		if(StringUtil.isEmpty(nsfPath)) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri); //$NON-NLS-1$
		}
		
		String mapKey = uri.getUserInfo() + nsfPath;
		FileSystem fs = fileSystems.get(mapKey);
		if(fs == null || !fs.isOpen()) {
			fileSystems.put(mapKey, new NSFFileSystem(this, uri.getUserInfo(), nsfPath));
		}
		return fileSystems.get(mapKey);
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		Objects.requireNonNull(uri, "uri cannot be null"); //$NON-NLS-1$
		
		String nsfPath = WebContentPathUtil.extractApiPath(uri);
		if(StringUtil.isEmpty(nsfPath)) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri); //$NON-NLS-1$
		}
		
		String mapKey = uri.getUserInfo() + nsfPath;
		return fileSystems.put(mapKey, new NSFFileSystem(this, uri.getUserInfo(), nsfPath));
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		Objects.requireNonNull(uri, "uri cannot be null"); //$NON-NLS-1$
		
		String nsfPath = WebContentPathUtil.extractApiPath(uri);
		if(StringUtil.isEmpty(nsfPath)) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri); //$NON-NLS-1$
		}
		
		String mapKey = uri.getUserInfo() + nsfPath;
		return fileSystems.get(mapKey);
	}

	@Override
	public Path getPath(URI uri) {
		return getFileSystem(uri).getPath(WebContentPathUtil.extractPathInfo(uri));
	}

	@Override
	public String toString() {
		return String.format("NSFFileSystemProvider [fileSystems=%s]", fileSystems); //$NON-NLS-1$
	}
}
