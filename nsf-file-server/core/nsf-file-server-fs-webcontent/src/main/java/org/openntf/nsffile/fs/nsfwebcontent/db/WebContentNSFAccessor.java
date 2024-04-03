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
package org.openntf.nsffile.fs.nsfwebcontent.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.hcl.domino.data.Database;
import com.hcl.domino.data.Document;
import com.hcl.domino.data.DocumentClass;
import com.hcl.domino.data.DominoDateTime;
import com.hcl.domino.data.UserData;
import com.hcl.domino.design.DesignElement;
import com.hcl.domino.design.DesignEntry;
import com.hcl.domino.design.FileResource;
import com.hcl.domino.misc.NotesConstants;
import com.hcl.domino.misc.Ref;

import org.openntf.nsffile.core.NotesPrincipal;
import org.openntf.nsffile.core.fs.attribute.NSFFileAttributes;
import org.openntf.nsffile.core.fs.attribute.NSFFileAttributes.Type;
import org.openntf.nsffile.core.util.NSFFileUtil;
import org.openntf.nsffile.fs.abstractnsf.NSFPath;
import org.openntf.nsffile.fs.abstractnsf.attribute.NSFUserDefinedFileAttributeView;
import org.openntf.nsffile.fs.abstractnsf.attribute.RootFileAttributes;
import org.openntf.nsffile.fs.abstractnsf.db.NSFAccessor;
import org.openntf.nsffile.fs.nsfwebcontent.util.WebContentPathUtil;

/**
 * Central class for NSF access methods.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public enum WebContentNSFAccessor implements NSFAccessor {
	instance;
	private static final Logger log = Logger.getLogger(WebContentNSFAccessor.class.getPackage().getName());
	
	/** The prefix used for user-defined items created this way */
	public static final String PREFIX_USERITEM = "user."; //$NON-NLS-1$
	/** The name of the custom data type used to store custom attributes */
	public static final String DATATYPE_NAME = NSFUserDefinedFileAttributeView.class.getSimpleName();
	
	/**
	 * Stores in-memory virtual folders "created" by {@link #createDirectory}
	 */
	private final Map<String, Collection<String>> virtualDirsPerNsf = new ConcurrentHashMap<>();
	
	@Override
	public List<String> getDirectoryEntries(NSFPath dir) {
		String cacheId = "entries-" + dir; //$NON-NLS-1$
		return WebContentPathUtil.callWithDatabase(dir, cacheId, database -> {
			String path = WebContentPathUtil.toFileName(dir);
			long slashes = countChars(path, '/');
			Set<String> result = streamWebContent(database)
				.map(DesignEntry::getTitle)
				// Should be in this dir or have at most one more slash than it
				.filter(p -> isInDir(path, p))
				.map(p -> trimDirEntry(path, p, slashes))
				.collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
			Collection<String> virtual = virtualDirsPerNsf.get(dir.getFileSystem().getNsfPath());
			if(virtual != null) {
				virtual.stream()
				.filter(p -> isInDir(path, p))
				.map(p -> trimDirEntry(path, p, slashes))
				.forEach(result::add);
			}
			return new ArrayList<>(result);
		});
	}
	
	private boolean isInDir(String dir, String path) {
		long pathSegments = countChars(path, '/');
		if(dir.isEmpty() && pathSegments <= 1) {
			return true;
		} else {
			return path.startsWith(dir+'/') && pathSegments > countChars(dir, '/');
		}
	}
	
	private String trimDirEntry(String path, String p, long slashes) {
		// If it's a file in a subdirectory, chomp it at the next level
		if(countChars(p, '/') > slashes) {
			int startIndex = path.isEmpty() ? 0 : path.length()+1;
			int nextIndex = p.indexOf('/', startIndex);
			return p.substring(startIndex, nextIndex == -1 ? p.length() : nextIndex);
		}
		return p;
	}
	
	@Override
	public Path extractAttachment(NSFPath path) {
		return WebContentPathUtil.callWithDatabase(path, null, database -> {
			String p = WebContentPathUtil.toFileName(path);
			Path result = NSFFileUtil.createTempFile();
			database.getDesign().getResourceAsStream(p).ifPresent(stream -> {
				try(InputStream is = stream) {
					Files.copy(is, result, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			
			return result;
		});
	}
	
	@Override
	public void storeAttachment(NSFPath path, Path attachmentData) throws IOException {
		WebContentPathUtil.runWithDatabase(path, database -> {
			String p = WebContentPathUtil.toFileName(path);
			Consumer<DesignElement> callback = element -> {
				FileResource res = (FileResource)element;
				res.setWebContentFile(true);
				res.setHideFromDesignList(true);
				res.setHideFromNotesVersion(4, true);
				res.sign();
				res.save();
			};
			try(OutputStream os = database.getDesign().newResourceOutputStream(p, callback)) {
				Files.copy(attachmentData, os);
			}
		});
	}
	
	@Override
	public void createDirectory(NSFPath dir, FileAttribute<?>... attrs) throws IOException {
		// Nothing to store in the NSF, but keep it in memory for later operations
		String nsfPath = dir.getFileSystem().getNsfPath();
		Collection<String> virtualDirs = virtualDirsPerNsf.computeIfAbsent(nsfPath, key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
		virtualDirs.add(WebContentPathUtil.toFileName(dir));
	}

	@Override
	public void delete(NSFPath path) throws IOException {
		WebContentPathUtil.runWithDatabase(path, database -> {
			String p = WebContentPathUtil.toFileName(path);
			Optional<FileResource> res = database.getDesign().getFileResource(p, true);
			if(res.isPresent()) {
				res.get().delete();
			} else {
				// Otherwise, it's likely a directory
				String nsfPath = path.getFileSystem().getNsfPath();
				Collection<String> virtualDirs = virtualDirsPerNsf.get(nsfPath);
				if(virtualDirs != null) {
					virtualDirs.remove(p);
				}
			}
		});
	}
	
	@Override
	public void copy(NSFPath source, NSFPath target, CopyOption... options) throws IOException {
		// TODO respect options
		storeAttachment(target, source);
	}

	@Override
	public void move(NSFPath source, NSFPath target, CopyOption... options) throws IOException {
		storeAttachment(target, source);
		Files.delete(source);
	}
	
	@Override
	public boolean exists(NSFPath path) {
		if("/".equals(path.toString())) { //$NON-NLS-1$
			return true;
		}
		
		// Could be a virtual directory created earlier
		Collection<String> virtualDirs = virtualDirsPerNsf.computeIfAbsent(path.getFileSystem().getNsfPath(), key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
		if(virtualDirs.contains(WebContentPathUtil.toFileName(path))) {
			return true;
		}
		
		String cacheId = "exists-" + path; //$NON-NLS-1$
		return WebContentPathUtil.callWithDatabase(path, cacheId, database -> {
			String p = WebContentPathUtil.toFileName(path);
			boolean regularFile = database.getDesign().getFileResource(p, true).isPresent();
			if(regularFile) {
				return true;
			} else {
				// Could be an implicit directory
				if(streamWebContent(database).map(DesignEntry::getTitle).anyMatch(p2 -> isInDir(p, p2))) {
					// Stash in the "virtualDirs", since this may be part of a "list and delete folders"
					//   operation
					virtualDirs.add(p);
					
					return true;
				} else {
					return false;
				}
			}
		});
	}

	@Override
	public NSFFileAttributes readAttributes(NSFPath path) {
		String cacheId = "attrs-" + path; //$NON-NLS-1$
		return WebContentPathUtil.callWithDocument(path, cacheId, doc -> {
			NotesPrincipal owner;
			NotesPrincipal group;
			Type type;
			FileTime lastModified;
			FileTime lastAccessed;
			FileTime created;
			long size;
			Set<PosixFilePermission> permissions;
			
			if(doc == null) {
				// Then it's a directory, which has no in-NSF representation
				owner = new NotesPrincipal("CN=root"); //$NON-NLS-1$
				group = new NotesPrincipal("CN=wheel"); //$NON-NLS-1$
				type = Type.Folder;
				lastModified = FileTime.from(Instant.EPOCH);
				lastAccessed = FileTime.from(Instant.EPOCH);
				created = FileTime.from(Instant.EPOCH);
				size = 0;
				permissions = EnumSet.allOf(PosixFilePermission.class);
			} else if(!doc.isNew()) {
				owner = new NotesPrincipal(doc.getSigner());
				group = new NotesPrincipal(doc.getSigner());
				type = Type.File;
				
				lastModified = FileTime.from(Instant.from(doc.getModifiedInThisFile()));
				
				// TODO check for minimum
				lastAccessed = FileTime.from(Instant.from(doc.getLastAccessed()));
				
				created = FileTime.from(Instant.from(doc.getCreated()));

				size = doc.getAsInt("$FileSize", 0); //$NON-NLS-1$
				
				permissions = PosixFilePermissions.fromString("rwx------"); //$NON-NLS-1$
			} else {
				owner = new NotesPrincipal("CN=root"); //$NON-NLS-1$
				group = new NotesPrincipal("CN=wheel"); //$NON-NLS-1$
				type = Type.File;
				lastModified = FileTime.from(Instant.EPOCH);
				lastAccessed = FileTime.from(Instant.EPOCH);
				created = FileTime.from(Instant.EPOCH);
				size = 0;
				permissions = EnumSet.allOf(PosixFilePermission.class);
			}
			
			return new NSFFileAttributes(owner, group, type, lastModified, lastAccessed, created, size, permissions);
		});
	}
	
	@Override
	public void setOwner(NSFPath path, UserPrincipal owner) throws IOException {
		// NOP
	}
	
	@Override
	public void setGroup(NSFPath path, UserPrincipal group) throws IOException {
		// NOP
	}
	
	@Override
	public void setPermissions(NSFPath path, Set<PosixFilePermission> perms) throws IOException {
		// NOP
	}
	
	@Override
	public void setTimes(NSFPath path, FileTime lastModifiedTime, FileTime createTime) throws IOException {
		// NOP
	}
	
	@Override
	public List<String> listUserDefinedAttributes(NSFPath path) throws IOException {
		try {
			String cacheId = "userAttrs-" + path; //$NON-NLS-1$
			return WebContentPathUtil.callWithDocument(path, cacheId, doc ->
				doc.getItemNames().stream()
					.filter(name -> name.startsWith(PREFIX_USERITEM) && name.length() > PREFIX_USERITEM.length())
					.map(name -> name.substring(PREFIX_USERITEM.length()))
					.collect(Collectors.toList())
			);
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception isting user-defined attributes for {0}", path), e);
			}
			throw new IOException(e);
		}
	}
	
	@Override
	public int writeUserDefinedAttribute(NSFPath path, String name, ByteBuffer src) throws IOException {
		try {
			return WebContentPathUtil.callWithDocument(path, null, doc -> {
				String itemName = PREFIX_USERITEM + name;
				byte[] data = src.array();
				UserData userData = doc.getParentDatabase().getParentDominoClient().createUserData(DATATYPE_NAME, data);
				doc.replaceItemValue(itemName, userData);
				doc.computeWithForm(true, null);
				WebContentPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
				return data.length;
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception setting user-defined attribute {0} on {1}", name, path), e);
			}
			throw new IOException(e);
		}
	}

	@Override
	public void deleteUserDefinedAttribute(NSFPath path, String name) throws IOException {
		try {
			WebContentPathUtil.runWithDocument(path, doc -> {
				String itemName = PREFIX_USERITEM + name;
				if(doc.hasItem(itemName)) {
					doc.removeItem(itemName);
					doc.computeWithForm(true, null);
					doc.save();
					WebContentPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
				}
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception deleting user-defined attribute {0} on {1}", name, path), e);
			}
			throw new IOException(e);
		}
	}
	
	@Override
	public byte[] getUserDefinedAttribute(NSFPath path, String name) throws IOException {
		try {
			String cacheId = "userAttrVal-" + path + name; //$NON-NLS-1$
			return WebContentPathUtil.callWithDocument(path, cacheId, doc -> {
				String itemName = PREFIX_USERITEM + name;
				return doc.getFirstItem(itemName)
					.map(item -> {
						switch(item.getType()) {
						case TYPE_TEXT:
						case TYPE_TEXT_LIST:
							return item.get(String.class, "").getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$
						case TYPE_USERDATA:
							UserData userData = item.get(UserData.class, null);
							return userData.getData();
						default:
							return new byte[0];
						}
					})
					.orElseGet(() -> new byte[0]);
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception retrieving user-defined attribute {0} of {1}", name, path), e);
			}
			throw new IOException(e);
		}
	}

	@Override
	public RootFileAttributes getRootFileAttributes(Path path) {
		return WebContentPathUtil.callWithDatabase((NSFPath)path, "rootAttribues", database -> { //$NON-NLS-1$
			Ref<DominoDateTime> mod = new Ref<>();
			database.getModifiedTime(mod, null);
			DominoDateTime created = database.getCreated();
			
			return new RootFileAttributes(Instant.from(mod.get()), Instant.from(created));
		});
	}

	/**
	 * Retrieves the document for the provided path, creating a new in-memory document
	 * if needed.
	 * 
	 * @param path the path to find the document for
	 * @param database the database housing the document
	 * @return a document representing the note
	 */
	public static Document getDocument(NSFPath path, Database database) {
		return database.getDesign().getFileResource(WebContentPathUtil.toFileName(path), true)
			.map(DesignElement::getDocument)
			.orElse(null);
	}
	
	private long countChars(String pool, char c) {
		return pool.chars()
			.filter(i -> i == c)
			.count();
	}
	
	private Stream<DesignEntry<DesignElement>> streamWebContent(Database database) {
		return database.getDesign()
			.getDesignEntries(EnumSet.of(DocumentClass.FORM), Collections.singleton(NotesConstants.DFLAGPAT_FILE_WEB))
			.filter(entry -> entry.getFlagsExt().contains(NotesConstants.DESIGN_FLAGEXT_WEBCONTENTFILE));
	}
}
