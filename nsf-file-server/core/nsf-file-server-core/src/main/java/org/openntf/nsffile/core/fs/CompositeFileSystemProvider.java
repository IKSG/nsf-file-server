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

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.fs.SftpPosixFileAttributes;

public class CompositeFileSystemProvider extends FileSystemProvider {
	public static final String SCHEME = "compositefs"; //$NON-NLS-1$
	public static final Logger log = Logger.getLogger(CompositeFileSystemProvider.class.getPackage().getName());
	
	public static final CompositeFileSystemProvider instance = new CompositeFileSystemProvider();
	
	public CompositeFileSystemProvider() {
		log.setLevel(Level.ALL);
	}

	// *******************************************************************************
	// * Filesystem Operations
	// *******************************************************************************

	@Override
	public String getScheme() {
		return SCHEME;
	}
	
	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		// TODO Figure out this whole deal
		throw new UnsupportedOperationException("Unable to getFileSystem for " + uri);
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		// TODO Figure out this whole deal
		throw new UnsupportedOperationException("Unable to getFileSystem for " + uri);
	}

	@Override
	public Path getPath(URI uri) {
		// TODO Figure out this whole deal
		throw new UnsupportedOperationException("Unable to getPath for " + uri);
	}
	
	// *******************************************************************************
	// * File Operations
	// *******************************************************************************

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException, AccessDeniedException {
		if("/".equals(path.toString())) { //$NON-NLS-1$
			if(modes != null && Arrays.asList(modes).contains(AccessMode.WRITE)) {
				throw new AccessDeniedException("Cannot write to the composite root");
			}
		} else {
			Path delegate = getDelegate(path);
			if(log.isLoggable(Level.FINEST)) {
				log.finest(format("Checking access for delegate {0} - {1}", delegate, Arrays.toString(modes)));
			}
			delegate.getFileSystem().provider().checkAccess(delegate, modes);
		}
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		// TODO support cross-FS copies
		Path sourceDelegate = getDelegate(source);
		Path targetDelegate = getDelegate(target);
		Files.copy(sourceDelegate, targetDelegate, options);
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		Path delegate = getDelegate(dir);
		Files.createDirectory(delegate, attrs);
	}

	@Override
	public void delete(Path path) throws IOException {
		Path delegate = getDelegate(path);
		Files.delete(delegate);
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		Path delegate = getDelegate(path);
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("Getting attributes view {0} for delegate {1}", type, path));
		}
		return Files.getFileAttributeView(delegate, type, options);
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		Path delegate = getDelegate(path);
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("Getting fileStore for delegate {0}", path));
		}
		return Files.getFileStore(delegate);
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		Path delegate = getDelegate(path);
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("isHidden {0}", delegate));
		}
		return Files.isHidden(delegate);
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return Objects.equals(path, path2);
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		// TODO support cross-FS moves
		Path sourceDelegate = getDelegate(source);
		Path targetDelegate = getDelegate(target);
		Files.move(sourceDelegate, targetDelegate, options);
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		Path delegate = getDelegate(path);
		return Files.newByteChannel(delegate, options, attrs);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("Opening directory stream for {0}", dir));
		}
		if("/".equals(dir.toString())) { //$NON-NLS-1$
			// Special case for the root
			CompositeFileSystem fileSystem = (CompositeFileSystem)dir.getFileSystem();
			return new CompositeDirectoryStream(fileSystem);
		}
		Path delegate = getDelegate(dir);
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("Getting newDirectoryStream for delegate {0}", delegate));
		}
		return Files.newDirectoryStream(delegate);
	}
	
	@Override
	public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		if("/".equals(path.toString())) { //$NON-NLS-1$
			throw new UnsupportedOperationException();
		}
		Path delegate = getDelegate(path);
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("Getting fileChannel for delegate {0}", delegate));
		}
		return delegate.getFileSystem().provider().newFileChannel(delegate, options, attrs);
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		if("/".equals(path.toString())) { //$NON-NLS-1$
			if(BasicFileAttributes.class.isAssignableFrom(type)) {
				return type.cast(RootFileAttributes.instance);
			} else if(PosixFileAttributes.class.isAssignableFrom(type)) {
				return type.cast(RootFileAttributes.instance);
			}
			return null;
		}
		Path delegate = getDelegate(path);
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("readAttributes type {0} for delegate {1}", type, delegate));
		}
		try {
			return Files.readAttributes(delegate, type, options);
		} catch(UnsupportedOperationException e) {
			// For Windows filesystem binds, since we declare POSIX compliance for SFTP purposes
			if(PosixFileAttributes.class.isAssignableFrom(type) && OsUtils.isWin32()) {
				return type.cast(new SftpPosixFileAttributes(path, new SftpClient.Attributes()));
			}
			throw e;
		}
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		if("/".equals(path.toString())) { //$NON-NLS-1$
			return Collections.emptyMap();
		}
		Path delegate = getDelegate(path);
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("readAttributes name {0} for delegate {1}", attributes, delegate));
		}
		try {
			return Files.readAttributes(delegate, attributes, options);
		} catch(UnsupportedOperationException e) {
			// This is common for things like an expectation of reading ZIP data for an NSF system.
			return Collections.emptyMap();
		}
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		if("/".equals(path.toString())) { //$NON-NLS-1$
			return;
		}
		Path delegate = getDelegate(path);
		if(log.isLoggable(Level.FINEST)) {
			log.finest(format("setAttribute name {0} for delegate {1}", attribute, delegate));
		}
		Files.setAttribute(delegate, attribute, value, options);
	}

	private Path getDelegate(Path path) {
		try {
			CompositeFileSystem compositeFileSystem = ((CompositePath)path).getFileSystem();
			Map<String, FileSystem> fileSystems = compositeFileSystem.getFileSystems();
			
			
			String mount = StreamSupport.stream(path.spliterator(), false)
				.filter(p -> !p.toString().isEmpty())
				.findFirst()
				.map(Path::toString)
				.orElseThrow(() -> new IllegalArgumentException(format("Unable to find subdirectory delegate for path {0}", path)));
			FileSystem fs = fileSystems.get(mount);
			if(fs == null) {
				throw new IllegalStateException(format("Unable to resolve mounted filesystem for \"{0}\"", mount));
			}
			
			String[] parts = StreamSupport.stream(path.spliterator(), false)
				.filter(p -> !p.toString().isEmpty())
				.skip(1)
				.map(Path::getFileName)
				.map(Object::toString)
				.toArray(String[]::new);
			Path result = fs.getPath("/", parts); //$NON-NLS-1$
			if(log.isLoggable(Level.FINEST)) {
				log.finest(format("Delegate path for {0} resolved to {1} in filesystem {2}", path, result, fs));
			}
			return result;
		} catch(Exception e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, format("Encountered exception trying to find delegate for {0}", path), e);
			}
			throw e;
		}
	}
}
