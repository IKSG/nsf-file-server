package org.openntf.nsffile.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Collection;

import org.apache.sshd.common.util.GenericUtils;

public class NSFFileStore extends FileStore {
	
	private final NSFFileSystem fileSystem;
	
	public NSFFileStore(NSFFileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	@Override
	public String name() {
		return fileSystem.getNsfPath();
	}

	@Override
	public String type() {
		return NSFFileSystemProvider.SCHEME;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public long getTotalSpace() throws IOException {
		// TODO look into checking quota
		return Long.MAX_VALUE;
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
        NSFFileSystemProvider provider = (NSFFileSystemProvider)fileSystem.provider();
        return provider.isSupportedFileAttributeView(fileSystem, type);
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
