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
package org.openntf.nsffile.fs.abstractnsf;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.commons.util.StringUtil;

import org.apache.sshd.common.util.GenericUtils;
import org.openntf.nsffile.core.fs.attribute.NoneFileAttributeView;
import org.openntf.nsffile.fs.abstractnsf.attribute.NSFPosixFileAttributeView;
import org.openntf.nsffile.fs.abstractnsf.attribute.NSFUserDefinedFileAttributeView;
import org.openntf.nsffile.fs.abstractnsf.db.NSFAccessor;

/**
 * Java NIO Filesystem implementation for NSF file storage.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public abstract class AbstractNSFFileSystemProvider extends FileSystemProvider {
	public static final Logger log = Logger.getLogger(AbstractNSFFileSystemProvider.class.getPackage().getName());
	
	private final NSFAccessor accessor;
	
	public AbstractNSFFileSystemProvider(NSFAccessor accessor) {
		this.accessor = accessor;
	}
	
	public NSFAccessor getAccessor() {
		return accessor;
	}
	
	// *******************************************************************************
	// * File Operations
	// *******************************************************************************

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		return newFileChannel(path, options, attrs);
	}
	
	@Override
	public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		return new NSFFileChannel(accessor, (NSFPath)path, options, attrs);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		return new NSFDirectoryStream(this, (NSFPath)dir);
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		if("".equals(dir.toString()) || "/".equals(dir.toString())) { //$NON-NLS-1$ //$NON-NLS-2$
			// Passively ignore
		} else {
			accessor.createDirectory((NSFPath)dir, attrs);
		}
	}

	@Override
	public void delete(Path path) throws IOException {
		accessor.delete((NSFPath)path);
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		accessor.copy((NSFPath)source, (NSFPath)target, options);
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		accessor.move((NSFPath)source, (NSFPath)target, options);
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		// TODO consider supporting symlinks
		return path.equals(path2);
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return false;
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		return path.getFileSystem().getFileStores().iterator().next();
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		if(!accessor.exists((NSFPath)path)) {
			throw new NoSuchFileException(path.toString());
		}
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		// TODO cache these?
		if(!accessor.exists((NSFPath)path)) {
			return type.cast(new NoneFileAttributeView(path));
		}
		if(type.isAssignableFrom(UserDefinedFileAttributeView.class)) {
			return type.cast(new NSFUserDefinedFileAttributeView(accessor, (NSFPath)path, options));
		} else {
			return type.cast(new NSFPosixFileAttributeView(accessor, (NSFPath)path, options));
		}
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		if("/".equals(path.toAbsolutePath().toString())) { //$NON-NLS-1$
			return type.cast(accessor.getRootFileAttributes(path));
		}
		if (type.isAssignableFrom(PosixFileAttributes.class)) {
			PosixFileAttributeView view = getFileAttributeView(path, PosixFileAttributeView.class, options);
			if(view == null) {
				throw new IOException("File does not exist: " + path); //$NON-NLS-1$
			}
            return type.cast(view.readAttributes());
        }
		return null;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		String view;
		String attrs;
		int i = attributes.indexOf(':');
		if (i == -1) {
			view = "basic"; //$NON-NLS-1$
			attrs = attributes;
		} else {
			view = attributes.substring(0, i++);
			attrs = attributes.substring(i);
		}

		return readAttributes(path, view, attrs, options);
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
	}
	
	// *******************************************************************************
	// * Links
	// *******************************************************************************
	
	@Override
	public void createLink(Path link, Path existing) throws IOException {
		super.createLink(link, existing);
	}
	
	@Override
	public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
		super.createSymbolicLink(link, target, attrs);
	}
	
	// *******************************************************************************
	// * Custom methods from Apache Mina
	// *******************************************************************************
	
	public Map<String, Object> readAttributes(Path path, String view, String attrs, LinkOption... options)
			throws IOException {
		NSFPath p = (NSFPath)path;
		NSFFileSystem fs = p.getFileSystem();
		Collection<String> views = fs.supportedFileAttributeViews();
		if (GenericUtils.isEmpty(views) || !views.contains(view)) {
			throw new UnsupportedOperationException(
			MessageFormat.format("readAttributes({0})[{1}:{2}] view not supported: {3}", path, view, attrs, views)); //$NON-NLS-1$
		}

		if ("basic".equalsIgnoreCase(view) || "posix".equalsIgnoreCase(view) || "owner".equalsIgnoreCase(view)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return readPosixViewAttributes(p, view, attrs, options);
		} else {
			return Collections.emptyMap();
		}
	}
	
	protected NavigableMap<String, Object> readPosixViewAttributes(NSFPath path, String view, String attrs,
			LinkOption... options) throws IOException {
		PosixFileAttributes v = readAttributes(path, PosixFileAttributes.class, options);
		if(v == null) {
			throw new IOException("File does not exist: " + path); //$NON-NLS-1$
		}
		if ("*".equals(attrs)) { //$NON-NLS-1$
			attrs = "lastModifiedTime,lastAccessTime,creationTime,size,isRegularFile,isDirectory,isSymbolicLink,isOther,fileKey,owner,permissions,group"; //$NON-NLS-1$
		}

		NavigableMap<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		boolean traceEnabled = log.isLoggable(Level.FINE);
		String[] attrValues = GenericUtils.split(attrs, ',');
		for (String attr : attrValues) {
			switch (attr) {
			case "lastModifiedTime": //$NON-NLS-1$
				map.put(attr, v.lastModifiedTime());
				break;
			case "lastAccessTime": //$NON-NLS-1$
				map.put(attr, v.lastAccessTime());
				break;
			case "creationTime": //$NON-NLS-1$
				map.put(attr, v.creationTime());
				break;
			case "size": //$NON-NLS-1$
				map.put(attr, v.size());
				break;
			case "isRegularFile": //$NON-NLS-1$
				map.put(attr, v.isRegularFile());
				break;
			case "isDirectory": //$NON-NLS-1$
				map.put(attr, v.isDirectory());
				break;
			case "isSymbolicLink": //$NON-NLS-1$
				map.put(attr, v.isSymbolicLink());
				break;
			case "isOther": //$NON-NLS-1$
				map.put(attr, v.isOther());
				break;
			case "fileKey": //$NON-NLS-1$
				map.put(attr, v.fileKey());
				break;
			case "owner": //$NON-NLS-1$
				map.put(attr, v.owner());
				break;
			case "permissions": //$NON-NLS-1$
				map.put(attr, v.permissions());
				break;
			case "group": //$NON-NLS-1$
				map.put(attr, v.group());
				break;
			default:
				if (traceEnabled) {
					log.fine(StringUtil.format("readPosixViewAttributes({0})[{1}:{2}] ignored for {3}", path, view, attr, attrs)); //$NON-NLS-1$
				}
			}
		}
		return map;
	}
	
	public boolean isSupportedFileAttributeView(Path path, Class<? extends FileAttributeView> type) {
        return isSupportedFileAttributeView(((NSFPath)path).getFileSystem(), type);
    }

    public boolean isSupportedFileAttributeView(NSFFileSystem fs, Class<? extends FileAttributeView> type) {
        Collection<String> views = fs.supportedFileAttributeViews();
        if ((type == null) || GenericUtils.isEmpty(views)) {
            return false;
        } else if (PosixFileAttributeView.class.isAssignableFrom(type)) {
            return views.contains("posix"); //$NON-NLS-1$
            // TODO support ACLs
//        } else if (AclFileAttributeView.class.isAssignableFrom(type)) {
//            return views.contains("acl");   // must come before owner view //$NON-NLS-1$
        } else if (FileOwnerAttributeView.class.isAssignableFrom(type)) {
            return views.contains("owner"); //$NON-NLS-1$
        } else if (BasicFileAttributeView.class.isAssignableFrom(type)) {
            return views.contains("basic"); // must be last //$NON-NLS-1$
        } else {
            return false;
        }
    }
}
