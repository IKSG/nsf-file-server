package org.openntf.nsffile.fs;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NonePosixFileAttributeView implements PosixFileAttributeView, BasicFileAttributeView, FileOwnerAttributeView {
	
	private final Path path;
	
	public NonePosixFileAttributeView(Path path) {
		this.path = path;
		
	}

	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
		throw new NoSuchFileException(path.toString());
	}

	@Override
	public UserPrincipal getOwner() throws IOException {
		throw new NoSuchFileException(path.toString());
	}

	@Override
	public void setOwner(UserPrincipal owner) throws IOException {
		throw new NoSuchFileException(path.toString());
	}

	@Override
	public String name() {
		return "posix";
	}

	@Override
	public PosixFileAttributes readAttributes() throws IOException {
		throw new NoSuchFileException(path.toString());
	}

	@Override
	public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
		throw new NoSuchFileException(path.toString());
	}

	@Override
	public void setGroup(GroupPrincipal group) throws IOException {
		throw new NoSuchFileException(path.toString());
	}

}
