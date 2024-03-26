package org.openntf.nsffile.commons.fs;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.apache.sshd.sftp.server.DefaultGroupPrincipal;
import org.apache.sshd.sftp.server.DefaultUserPrincipal;

public class RootFileAttributes implements PosixFileAttributes {
	public static final RootFileAttributes instance = new RootFileAttributes();

	@Override
	public FileTime lastModifiedTime() {
		return FileTime.from(Instant.now());
	}

	@Override
	public FileTime lastAccessTime() {
		return FileTime.from(Instant.now());
	}

	@Override
	public FileTime creationTime() {
		return FileTime.from(Instant.EPOCH);
	}

	@Override
	public boolean isRegularFile() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public boolean isSymbolicLink() {
		return false;
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public long size() {
		return 0;
	}

	@Override
	public Object fileKey() {
		return instance;
	}

	@Override
	public UserPrincipal owner() {
		return new DefaultUserPrincipal("root"); //$NON-NLS-1$
	}

	@Override
	public GroupPrincipal group() {
		return new DefaultGroupPrincipal("wheel"); //$NON-NLS-1$
	}

	@Override
	public Set<PosixFilePermission> permissions() {
		return EnumSet.of(
			PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
			PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
			PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
		);
	}

}
