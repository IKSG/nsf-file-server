package org.openntf.nsffile.fs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.server.subsystem.sftp.DefaultGroupPrincipal;
import org.apache.sshd.server.subsystem.sftp.DefaultUserPrincipal;
import org.openntf.nsffile.util.NotesThreadFactory;

import lotus.domino.Document;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NSFFileAttributes implements BasicFileAttributes, PosixFileAttributes {
	private enum Type {
		File, Folder
	}
	
	private String owner;
	private String group;
	private Type type;
	private long lastModified;
	private long lastAccessed;
	private long created;
	private long size;
	
	public NSFFileAttributes(NSFFileSystemProvider provider, NSFPath path) {
		try {
			NotesThreadFactory.executor.submit(() -> {
				try {
					Document doc = provider.getDocument(path);
					@SuppressWarnings("unchecked")
					List<String> updatedBy = doc.getItemValue("$UpdatedBy");
					owner = provider.shortCn(updatedBy.get(0));
					group = "wheel"; // TODO implement
					type = Type.valueOf(doc.getItemValueString("Form"));
					lastModified = doc.getLastModified().toJavaDate().getTime();
					lastAccessed = doc.getLastAccessed().toJavaDate().getTime();
					created = doc.getCreated().toJavaDate().getTime();
					size = doc.getSize();
				} catch(Throwable t) {
					t.printStackTrace(System.out);
					throw t;
				}
				
				return null;
			}).get();
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public UserPrincipal owner() {
		return new DefaultUserPrincipal(owner);
	}

	@Override
	public GroupPrincipal group() {
		return new DefaultGroupPrincipal(group);
	}

	@Override
	public Set<PosixFilePermission> permissions() {
		return EnumSet.allOf(PosixFilePermission.class);
	}

	@Override
	public FileTime lastModifiedTime() {
		return FileTime.from(lastModified, TimeUnit.MILLISECONDS);
	}

	@Override
	public FileTime lastAccessTime() {
		return FileTime.from(lastAccessed, TimeUnit.MILLISECONDS);
	}

	@Override
	public FileTime creationTime() {
		return FileTime.from(created, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean isRegularFile() {
		return type == Type.File;
	}

	@Override
	public boolean isDirectory() {
		return type == Type.Folder;
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
		return type == Type.File ? size : 0;
	}

	@Override
	public Object fileKey() {
		return null;
	}

}
