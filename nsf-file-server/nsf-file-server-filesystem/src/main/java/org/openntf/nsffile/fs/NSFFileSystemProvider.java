package org.openntf.nsffile.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.common.util.GenericUtils;
import org.openntf.nsffile.util.NSFPathUtil;
import org.openntf.nsffile.util.NotesThreadFactory;
import org.openntf.nsffile.util.SudoUtils;

import com.ibm.commons.util.StringUtil;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.Session;
import lotus.domino.View;

/**
 * Java NIO Filesystem implementation for NSF file storage.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NSFFileSystemProvider extends FileSystemProvider {
	public static final String SCHEME = "nsffilestore";
	public static final Logger log = Logger.getLogger(NSFFileSystemProvider.class.getPackage().getName());
	
	public static final NSFFileSystemProvider instance = new NSFFileSystemProvider();
	
	private Map<String, FileSystem> fileSystems = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	@Override
	public String getScheme() {
		return SCHEME;
	}
	
	public FileSystem getOrCreateFileSystem(URI uri, Map<String, ?> env) throws IOException {
		Objects.requireNonNull(uri, "uri cannot be null");
		
		String nsfPath = NSFPathUtil.extractApiPath(uri);
		if(StringUtil.isEmpty(nsfPath)) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri);
		}
		
		String mapKey = uri.getUserInfo() + nsfPath;
		FileSystem fs = fileSystems.get(mapKey);
		if(fs == null || !fs.isOpen()) {
			fileSystems.put(mapKey,new NSFFileSystem(this, uri.getUserInfo(), nsfPath));
		}
		return fileSystems.get(mapKey);
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		Objects.requireNonNull(uri, "uri cannot be null");
		
		String nsfPath = NSFPathUtil.extractApiPath(uri);
		if(StringUtil.isEmpty(nsfPath)) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri);
		}
		
		String mapKey = uri.getUserInfo() + nsfPath;
		return fileSystems.put(mapKey, new NSFFileSystem(this, uri.getUserInfo(), nsfPath));
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		Objects.requireNonNull(uri, "uri cannot be null");
		
		String nsfPath = NSFPathUtil.extractApiPath(uri);
		if(StringUtil.isEmpty(nsfPath)) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri);
		}
		
		String mapKey = uri.getUserInfo() + nsfPath;
		return fileSystems.get(mapKey);
	}

	@Override
	public Path getPath(URI uri) {
		return getFileSystem(uri).getPath(NSFPathUtil.extractPathInfo(uri));
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		System.out.println("newByteChannel for " + path);
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		return new NSFFileChannel(this, (NSFPath)path, options, attrs);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		return new NSFDirectoryStream(this, (NSFPath)dir);
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		System.out.println("createDirectory " + dir);
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Path path) throws IOException {
		try {
			NotesThreadFactory.executor.submit(() -> {
				Document doc = getDocument((NSFPath)path);
				if(!doc.isNewNote()) {
					if(doc.getParentDatabase().isDocumentLockingEnabled()) {
						doc.lock();
					}
					doc.remove(false);
				}
				return null;
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		System.out.println("copy " + source + " -> " + target);
		// TODO Auto-generated method stub

	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		System.out.println("move " + source + " -> " + target);
		// TODO Auto-generated method stub

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
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		// TODO cache these?
		return type.cast(new NSFPosixFileAttributeView(this, (NSFPath)path, options));
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		if("/".equals(path.toAbsolutePath().toString())) {
			return type.cast(new RootFileAttributes());
		}
		if (type.isAssignableFrom(PosixFileAttributes.class)) {
            return type.cast(getFileAttributeView(path, PosixFileAttributeView.class, options).readAttributes());
        }
		return null;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		String view;
		String attrs;
		int i = attributes.indexOf(':');
		if (i == -1) {
			view = "basic";
			attrs = attributes;
		} else {
			view = attributes.substring(0, i++);
			attrs = attributes.substring(i);
		}

		return readAttributes(path, view, attrs, options);
	}
	
	public Map<String, Object> readAttributes(Path path, String view, String attrs, LinkOption... options)
			throws IOException {
		NSFPath p = (NSFPath)path;
		NSFFileSystem fs = p.getFileSystem();
		Collection<String> views = fs.supportedFileAttributeViews();
		if (GenericUtils.isEmpty(views) || (!views.contains(view))) {
			throw new UnsupportedOperationException(
					"readAttributes(" + path + ")[" + view + ":" + attrs + "] view not supported: " + views);
		}

		if ("basic".equalsIgnoreCase(view) || "posix".equalsIgnoreCase(view) || "owner".equalsIgnoreCase(view)) {
			return readPosixViewAttributes(p, view, attrs, options);
		} else {
			return Collections.emptyMap();
		}
	}
	
	protected NavigableMap<String, Object> readPosixViewAttributes(NSFPath path, String view, String attrs,
			LinkOption... options) throws IOException {
		PosixFileAttributes v = readAttributes(path, PosixFileAttributes.class, options);
		if ("*".equals(attrs)) {
			attrs = "lastModifiedTime,lastAccessTime,creationTime,size,isRegularFile,isDirectory,isSymbolicLink,isOther,fileKey,owner,permissions,group";
		}

		NavigableMap<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		boolean traceEnabled = log.isLoggable(Level.FINE);
		String[] attrValues = GenericUtils.split(attrs, ',');
		for (String attr : attrValues) {
			switch (attr) {
			case "lastModifiedTime":
				map.put(attr, v.lastModifiedTime());
				break;
			case "lastAccessTime":
				map.put(attr, v.lastAccessTime());
				break;
			case "creationTime":
				map.put(attr, v.creationTime());
				break;
			case "size":
				map.put(attr, v.size());
				break;
			case "isRegularFile":
				map.put(attr, v.isRegularFile());
				break;
			case "isDirectory":
				map.put(attr, v.isDirectory());
				break;
			case "isSymbolicLink":
				map.put(attr, v.isSymbolicLink());
				break;
			case "isOther":
				map.put(attr, v.isOther());
				break;
			case "fileKey":
				map.put(attr, v.fileKey());
				break;
			case "owner":
				map.put(attr, v.owner());
				break;
			case "permissions":
				map.put(attr, v.permissions());
				break;
			case "group":
				map.put(attr, v.group());
				break;
			default:
				if (traceEnabled) {
					log.fine(StringUtil.format("readPosixViewAttributes({0})[{1}:{2}] ignored for {3}", path, view, attr, attrs));
				}
			}
		}
		return map;
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub

	}
	
	// *******************************************************************************
	// * Custom methods from Apache Mina
	// *******************************************************************************
	
	public boolean isSupportedFileAttributeView(Path path, Class<? extends FileAttributeView> type) {
        return isSupportedFileAttributeView(((NSFPath)path).getFileSystem(), type);
    }

    public boolean isSupportedFileAttributeView(NSFFileSystem fs, Class<? extends FileAttributeView> type) {
        Collection<String> views = fs.supportedFileAttributeViews();
        if ((type == null) || GenericUtils.isEmpty(views)) {
            return false;
        } else if (PosixFileAttributeView.class.isAssignableFrom(type)) {
            return views.contains("posix");
        } else if (AclFileAttributeView.class.isAssignableFrom(type)) {
            return views.contains("acl");   // must come before owner view
        } else if (FileOwnerAttributeView.class.isAssignableFrom(type)) {
            return views.contains("owner");
        } else if (BasicFileAttributeView.class.isAssignableFrom(type)) {
            return views.contains("basic"); // must be last
        } else {
            return false;
        }
    }
	
	// *******************************************************************************
	// * Internal utility methods
	// *******************************************************************************

	public Database getDatabase(NSFFileSystem fileSystem) {
		try {
			String userName = fileSystem.getUserName();
			String nsfPath = fileSystem.getNsfPath();
			
			Session session = SudoUtils.getSessionAs(dn(userName), null);
			
			int bangIndex = nsfPath.indexOf("!!");
			if(bangIndex < 0) {
				return session.getDatabase("", nsfPath);
			} else {
				return session.getDatabase(nsfPath.substring(0, bangIndex), nsfPath.substring(bangIndex+2));
			}
		} catch(NotesException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public Document getDocument(NSFPath path) {
		try {
			Database database = getDatabase(path.getFileSystem());
			View view = database.getView("Files by Path");
			view.setAutoUpdate(false);
			Document doc = view.getDocumentByKey(path.toAbsolutePath().toString(), true);
			if(doc == null) {
				doc = database.createDocument();
				doc.replaceItemValue("Form", "File");
				doc.replaceItemValue("Parent", path.getParent().toAbsolutePath().toString());
				doc.replaceItemValue("$$Title", path.getFileName().toString());
			}
			return doc;
		} catch(NotesException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	private String dn(String name) {
		try {
			return NotesThreadFactory.executor.submit(() -> {
				Session s = NotesFactory.createSession();
				try {
					return s.createName(name).getCanonical();
				} finally {
					s.recycle();
				}
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public String shortCn(String name) {
		try {
			return NotesThreadFactory.executor.submit(() -> {
				Session s = NotesFactory.createSession();
				try {
					return s.createName(name).getCommon().replaceAll("\\s+", "");
				} finally {
					s.recycle();
				}
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
