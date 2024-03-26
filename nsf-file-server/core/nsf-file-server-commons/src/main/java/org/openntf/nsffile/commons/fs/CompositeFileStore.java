package org.openntf.nsffile.commons.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Collection;
import java.util.stream.StreamSupport;

import org.apache.sshd.common.util.GenericUtils;

public class CompositeFileStore extends FileStore {
	
	private final CompositeFileSystem fileSystem;
	
	public CompositeFileStore(final CompositeFileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	@Override
	public String name() {
		return fileSystem.getClass().getSimpleName();
	}

	@Override
	public String type() {
		return CompositeFileSystemProvider.SCHEME;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public long getTotalSpace() throws IOException {
		return this.fileSystem.getFileSystems().values()
			.stream()
			.flatMap(fs -> StreamSupport.stream(fs.getFileStores().spliterator(), false))
			.mapToLong(value -> {
				try {
					return value.getTotalSpace();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			})
			.sum();
	}

	@Override
	public long getUsableSpace() throws IOException {
		return Long.MAX_VALUE;
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		return Long.MAX_VALUE;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		for(FileSystem fileSystem : this.fileSystem.getFileSystems().values()) {
			Collection<String> views = fileSystem.supportedFileAttributeViews();
	        if ((type == null) || GenericUtils.isEmpty(views)) {
	            return false;
	        } else if (PosixFileAttributeView.class.isAssignableFrom(type)) {
	            return views.contains("posix"); //$NON-NLS-1$
	            // TODO support ACLs
//	        } else if (AclFileAttributeView.class.isAssignableFrom(type)) {
//	            return views.contains("acl");   // must come before owner view //$NON-NLS-1$
	        } else if (FileOwnerAttributeView.class.isAssignableFrom(type)) {
	            return views.contains("owner"); //$NON-NLS-1$
	        } else if (BasicFileAttributeView.class.isAssignableFrom(type)) {
	            return views.contains("basic"); // must be last //$NON-NLS-1$
	        } else {
	            return false;
	        }
		}
		return true;
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		Collection<String> views = fileSystem.supportedFileAttributeViews();
        return !GenericUtils.isEmpty(views) && views.contains(name);
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		return null;
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		return null;
	}

}
