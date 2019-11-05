package org.openntf.nsffile.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sshd.client.subsystem.sftp.fs.SftpFileSystem.DefaultUserPrincipalLookupService;
import org.apache.sshd.common.file.util.BaseFileSystem;

public class NSFFileSystem extends BaseFileSystem<NSFPath> {
	
	private final String userName;
	private final String nsfPath;
	private final List<FileStore> fileStores;
	
	public NSFFileSystem(NSFFileSystemProvider provider, String userName, String nsfPath) {
		super(provider);
		
		this.userName = userName;
		this.nsfPath = nsfPath;
		this.fileStores = Arrays.asList(new NSFFileStore(this));
	}

	@Override
	protected NSFPath create(String root, List<String> names) {
		return new NSFPath(this, root, names);
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return new HashSet<>(Arrays.asList("basic", "posix", "owner"));
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		return DefaultUserPrincipalLookupService.INSTANCE;
	}
	
	@Override
	public Iterable<FileStore> getFileStores() {
		return fileStores;
	}
	
	// *******************************************************************************
	// * Domino-specific methods
	// *******************************************************************************
	
	public String getUserName() {
		return userName;
	}
	
	public String getNsfPath() {
		return nsfPath;
	}
}
