package org.openntf.nsffile.fs;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

public class NSFPosixFileAttributeView implements PosixFileAttributeView, BasicFileAttributeView, FileOwnerAttributeView {
	
	private final NSFFileAttributes attrs;
	
	public NSFPosixFileAttributeView(NSFFileSystemProvider provider, NSFPath path, LinkOption... options) {
        this.attrs = new NSFFileAttributes(provider, path);
    }

	@Override
	public String name() {
		return "posix";
	}

	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public UserPrincipal getOwner() throws IOException {
		return this.attrs.owner();
	}

	@Override
	public void setOwner(UserPrincipal owner) throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public PosixFileAttributes readAttributes() throws IOException {
		return this.attrs;
	}

	@Override
	public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setGroup(GroupPrincipal group) throws IOException {
		// TODO Auto-generated method stub

	}

}
