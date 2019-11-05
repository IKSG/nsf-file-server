package org.openntf.nsffile.fs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.apache.sshd.server.subsystem.sftp.DefaultGroupPrincipal;
import org.apache.sshd.server.subsystem.sftp.DefaultUserPrincipal;

public class RootFileAttributes implements BasicFileAttributes, PosixFileAttributes {
	
	// TODO read from DB

	@Override
	public UserPrincipal owner() {
		return new DefaultUserPrincipal("root");
	}

	@Override
	public GroupPrincipal group() {
		return new DefaultGroupPrincipal("wheel");
	}

	@Override
	public Set<PosixFilePermission> permissions() {
		return EnumSet.allOf(PosixFilePermission.class);
	}

	@Override
	public FileTime lastModifiedTime() {
		return FileTime.from(Instant.EPOCH);
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
		return null;
	}

}
