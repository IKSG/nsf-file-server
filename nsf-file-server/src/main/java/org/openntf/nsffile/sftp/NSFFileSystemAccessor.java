package org.openntf.nsffile.sftp;

import java.util.Objects;

import org.apache.sshd.server.subsystem.sftp.SftpFileSystemAccessor;

public class NSFFileSystemAccessor implements SftpFileSystemAccessor {
	private final String nsfPath;
	
	public NSFFileSystemAccessor(String nsfPath) {
		this.nsfPath = Objects.requireNonNull(nsfPath, "nsfPath cannot be null");
	}
}
