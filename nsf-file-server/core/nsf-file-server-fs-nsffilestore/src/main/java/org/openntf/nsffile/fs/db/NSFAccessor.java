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
package org.openntf.nsffile.fs.db;

import static org.openntf.nsffile.fs.NSFFileSystemConstants.DATATYPE_NAME;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.FORM_FOLDER;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.ITEM_CREATED;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.ITEM_FILE;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.ITEM_GROUP;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.ITEM_MODIFIED;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.ITEM_OWNER;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.ITEM_PARENT;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.ITEM_PERMISSIONS;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.PREFIX_USERITEM;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.VIEW_FILESBYPARENT;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.VIEW_FILESBYPARENT_INDEX_NAME;
import static org.openntf.nsffile.fs.NSFFileSystemConstants.VIEW_FILESBYPATH;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.hcl.domino.data.Attachment;
import com.hcl.domino.data.Attachment.Compression;
import com.hcl.domino.data.CollectionEntry;
import com.hcl.domino.data.CollectionSearchQuery.CollectionEntryProcessor;
import com.hcl.domino.data.Database;
import com.hcl.domino.data.Database.Action;
import com.hcl.domino.data.Document;
import com.hcl.domino.data.Document.LockMode;
import com.hcl.domino.data.DominoCollection;
import com.hcl.domino.data.UserData;
import com.hcl.domino.misc.NotesConstants;
import com.hcl.domino.richtext.RichTextWriter;
import com.ibm.commons.util.StringUtil;

import org.openntf.nsffile.core.NotesPrincipal;
import org.openntf.nsffile.core.fs.attribute.NSFFileAttributes;
import org.openntf.nsffile.core.fs.attribute.NSFFileAttributes.Type;
import org.openntf.nsffile.core.util.NSFFileUtil;
import org.openntf.nsffile.fs.NSFPath;
import org.openntf.nsffile.fs.util.NSFPathUtil;

/**
 * Central class for NSF access methods.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public enum NSFAccessor {
	;
	private static final Logger log = Logger.getLogger(NSFAccessor.class.getPackage().getName());
	
	/**
	 * Returns a list of file names for files within the provided directory.
	 * 
	 * @param dir the directory to list
	 * @return a {@link List} of individual file names, in alphabetical order
	 */
	public static List<String> getDirectoryEntries(NSFPath dir) {
		String cacheId = "entries-" + dir; //$NON-NLS-1$
		return NSFPathUtil.callWithDatabase(dir, cacheId, database -> {
			DominoCollection filesByParent = database.openCollection(VIEW_FILESBYPARENT)
				.orElseThrow(() -> new IllegalStateException(MessageFormat.format("Unable to open view \"{0}\" in database \"{1}\"", VIEW_FILESBYPARENT, database.getRelativeFilePath())));;
			filesByParent.refresh();
			
			String category = dir.toAbsolutePath().toString();
			return filesByParent.query()
				.startAtCategory(category)
				.readColumnValues()
				.build(0, Integer.MAX_VALUE, new CollectionEntryProcessor<List<String>>() {

					@Override
					public List<String> start() {
						return new ArrayList<>();
					}

					@Override
					public Action entryRead(List<String> result, CollectionEntry entry) {
						result.add(entry.get(VIEW_FILESBYPARENT_INDEX_NAME, String.class, "")); //$NON-NLS-1$
						return Action.Continue;
					}
					@Override
					public List<String> end(List<String> result) {
						return result;
					}
				});
		});
	}
	
	/**
	 * Extracts the attachment from the provided NSF path.
	 * 
	 * <p>The extracted file will have the same name as the {code path}'s file name, and
	 * will be housed within a temporary directory. Both the returned file and its parent
	 * should be deleted when no longer needed.</p>
	 * 
	 * @param path the path of the file to extract
	 * @return a {@link Path} to a temporary file holding the attachment contents
	 */
	public static Path extractAttachment(NSFPath path) {
		return NSFPathUtil.callWithDocument(path, null, doc -> {
			Path resultParent = NSFFileUtil.createTempDirectory(path.getFileName().toString());
			Path result = resultParent.resolve(path.getFileName().toString());
			if(doc.hasItem(ITEM_FILE)) {
				// TODO add sanity checks
				doc.forEachAttachment((attachment, loop) -> {
					try(InputStream is = attachment.getInputStream()) {
						Files.copy(is, result, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						throw new UncheckedIOException("Encountered exception extracting attachment data", e);
					}
					loop.stop();
				});
			} else {
				Files.createFile(result);
			}
			
			return result;
		});
	}
	
	/**
	 * Stores the provided attachment data in the named path.
	 * 
	 * @param path the path to the file inside the NSF
	 * @param attachmentData the path to the attachment data stored on disk
	 * @throws IOException if there is a problem attaching the data
	 */
	public static void storeAttachment(NSFPath path, Path attachmentData) throws IOException {
		try {
			NSFPathUtil.runWithDocument(path, doc -> {
				if(doc.isNew()) {
					doc.replaceItemValue(NotesConstants.FIELD_FORM, ITEM_FILE);
				}
				if(doc.hasItem(ITEM_FILE)) {
					doc.removeItem(ITEM_FILE);
				}
				Attachment att = doc.attachFile(attachmentData.toAbsolutePath().toString(), attachmentData.getFileName().toString(), Compression.NONE);
				try(RichTextWriter w = doc.createRichTextItem(ITEM_FILE)) {
					w.addAttachmentIcon(att, attachmentData.getFileName().toString());
				}
				doc.computeWithForm(true, null);
				doc.save();
				NSFPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
			});
		} catch (RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception storing attachment in path {0}", path), e);
			}
			throw new IOException(e);
		}
	}
	
	/**
	 * Creates a directory entry for the provided path, if it doesn't currently exist.
	 * 
	 * @param dir the path of the desired directory
	 * @param attrs the attributes of the directory to create
	 * @throws IOException if there is a problem creating the directory document
	 */
	public static void createDirectory(NSFPath dir, FileAttribute<?>... attrs) throws IOException {
		// TODO support attrs
		try {
			NSFPathUtil.runWithDocument(dir, doc -> {
				if(doc.isNew()) {
					doc.replaceItemValue(NotesConstants.FIELD_FORM, FORM_FOLDER);
					doc.computeWithForm(true, null);
					doc.save();
					NSFPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
				}
			});
		} catch (RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception creating directory {0}", dir), e);
			}
			throw new IOException(e);
		}
	}

	/**
	 * Deletes the file or folder at the provided path, if it exists.
	 * 
	 * @param path the path of the file to delete
	 * @throws IOException if there is a problem deleting the file
	 */
	public static void delete(NSFPath path) throws IOException {
		// TODO throw exception if it is a non-empty directory
		try {
			NSFPathUtil.runWithDocument((NSFPath)path, doc -> {
				if(!doc.isNew()) {
					if(doc.getParentDatabase().isDocumentLockingEnabled()) {
						doc.lock(doc.getParentDatabase().getParentDominoClient().getEffectiveUserName(), LockMode.HardOrProvisional);
					}
					Database db = doc.getParentDatabase();
					doc.delete();
					NSFPathUtil.invalidateDatabaseCache(db);
				}
			});
		} catch (RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception deleting path {0}", path), e);
			}
			throw new IOException(e);
		}
	}
	
	/**
	 * Copies the provided source note to the target, deleting the target if it exists.
	 * 
	 * @param source the source path to copy
	 * @param target the target path
	 * @param options Java NIO copy options
	 * @throws IOException if there is a database problem copying the file
	 */
	public static void copy(NSFPath source, NSFPath target, CopyOption... options) throws IOException {
		// TODO respect options
		try {
			NSFPathUtil.runWithDatabase(source, database -> {
				Document targetDoc = NSFAccessor.getDocument(target, database);
				if(!targetDoc.isNew()) {
					if(targetDoc.getParentDatabase().isDocumentLockingEnabled()) {
						targetDoc.lock(targetDoc.getParentDatabase().getParentDominoClient().getEffectiveUserName(), LockMode.HardOrProvisional);
					}
					targetDoc.delete();
				}
				
				Document doc = NSFAccessor.getDocument(source, database);
				targetDoc = doc.copyToDatabase(database);
				targetDoc.replaceItemValue(ITEM_PARENT, target.getParent().toAbsolutePath().toString());
				targetDoc.replaceItemValue(NotesConstants.ITEM_META_TITLE, target.getFileName().toString());
				targetDoc.computeWithForm(true, null);
				targetDoc.save();
				NSFPathUtil.invalidateDatabaseCache(database);
			});
		} catch (RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception copying {0} to {1} with options {2}", source, target, Arrays.toString(options)), e);
			}
			throw new IOException(e);
		}
	}

	/**
	 * Moves the provided source note to the target, deleting the target if it exists.
	 * 
	 * @param source the source path to move
	 * @param target the target path
	 * @param options Java NIO copy options
	 * @throws IOException if there is a database problem moving the file
	 */
	public static void move(NSFPath source, NSFPath target, CopyOption... options) throws IOException {
		try {
			NSFPathUtil.runWithDatabase(source, database -> {
				Document targetDoc = NSFAccessor.getDocument(target, database);
				if(!targetDoc.isNew()) {
					if(targetDoc.getParentDatabase().isDocumentLockingEnabled()) {
						targetDoc.lock(targetDoc.getParentDatabase().getParentDominoClient().getEffectiveUserName(), LockMode.HardOrProvisional);
					}
					targetDoc.delete();
				}
				
				Document doc = NSFAccessor.getDocument((NSFPath)source, database);
				doc.replaceItemValue(ITEM_PARENT, target.getParent().toAbsolutePath().toString());
				doc.replaceItemValue(NotesConstants.ITEM_META_TITLE, target.getFileName().toString());
				doc.computeWithForm(true, null);
				doc.save();
				NSFPathUtil.invalidateDatabaseCache(database);
			});
		} catch (RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception moving {0} to {1} with options {2}", source, target, Arrays.toString(options)), e);
			}
			throw new IOException(e);
		}
	}
	
	/**
	 * Checks if the provided path exists in the database.
	 * 
	 * @param path the path of the file or folder to check
	 * @return whether the file currently exists in the database
	 */
	public static boolean exists(NSFPath path) {
		if("/".equals(path.toString())) { //$NON-NLS-1$
			return true;
		}
		String cacheId = "exists-" + path; //$NON-NLS-1$
		return NSFPathUtil.callWithDatabase(path, cacheId, database -> {
			DominoCollection view = database.openCollection(VIEW_FILESBYPATH)
				.orElseThrow(() -> new IllegalStateException(MessageFormat.format("Unable to open view \"{0}\" in database \"{1}\"", VIEW_FILESBYPATH, database.getRelativeFilePath())));;
			view.refresh();
			return view.query()
				.selectByKey(path.toAbsolutePath().toString(), true)
				.firstId()
				.isPresent();
		});
	}
	
	public static NSFFileAttributes readAttributes(NSFPath path) {
		String cacheId = "attrs-" + path; //$NON-NLS-1$
		return NSFPathUtil.callWithDocument(path, cacheId, doc -> {
			NotesPrincipal owner;
			NotesPrincipal group;
			Type type;
			FileTime lastModified;
			FileTime lastAccessed;
			FileTime created;
			long size;
			Set<PosixFilePermission> permissions;
			
			if(!doc.isNew()) {
				owner = new NotesPrincipal(doc.get(ITEM_OWNER, String.class, "")); //$NON-NLS-1$
				group = new NotesPrincipal(doc.get(ITEM_GROUP, String.class, "")); //$NON-NLS-1$
				
				String form = doc.get(NotesConstants.FIELD_FORM, String.class, null);
				if(StringUtil.isNotEmpty(form)) {
					type = Type.valueOf(form);
				} else {
					type = null;
				}
				Instant mod = doc.get(ITEM_MODIFIED, Instant.class, Instant.now());
				lastModified = FileTime.from(mod);
				
				// TODO check for minimum
				lastAccessed = FileTime.from(Instant.from(doc.getLastAccessed()));
				
				Instant docCreated = doc.get(ITEM_CREATED, Instant.class, Instant.from(doc.getCreated()));
				created = FileTime.from(docCreated);

				size = doc.getAttachmentNames()
					.stream()
					.findFirst()
					.flatMap(name -> doc.getAttachment(name))
					.map(Attachment::getFileSize)
					.orElse(0l);
				
				permissions = PosixFilePermissions.fromString(doc.get(ITEM_PERMISSIONS, String.class, "")); //$NON-NLS-1$
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
	
	/**
	 * Sets the owner of the provided path to the provided name.
	 * 
	 * @param path the path of the file or folder to set
	 * @param owner the new owner name
	 * @throws IOException if there is a database problem setting the owner
	 */
	public static void setOwner(NSFPath path, UserPrincipal owner) throws IOException {
		try {
			NSFPathUtil.runWithDocument(path, doc -> {
				doc.replaceItemValue(ITEM_OWNER, owner.getName());
				doc.save();
				NSFPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception setting owner of {0} to {1}", path, owner), e);
			}
			throw new IOException(e);
		}
	}
	
	/**
	 * Sets the group of the provided path to the provided name.
	 * 
	 * @param path the path of the file or folder to set
	 * @param group the new group name
	 * @throws IOException if there is a database problem setting the group
	 */
	public static void setGroup(NSFPath path, UserPrincipal group) throws IOException {
		try {
			NSFPathUtil.runWithDocument(path, doc -> {
				doc.replaceItemValue(ITEM_GROUP, group.getName());
				doc.save();
				NSFPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception setting group of {0} to {1}", path, group), e);
			}
			throw new IOException(e);
		}
	}
	
	/**
	 * Sets the permissions of the provided path to the provided name.
	 * 
	 * @param path the path of the file or folder to set
	 * @param perms the new permissions
	 * @throws IOException if there is a database problem setting the permissions
	 */
	public static void setPermissions(NSFPath path, Set<PosixFilePermission> perms) throws IOException {
		try {
			NSFPathUtil.runWithDocument(path, doc -> {
				doc.replaceItemValue(ITEM_PERMISSIONS, PosixFilePermissions.toString(perms));
				doc.save();
				NSFPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception setting permissions of {0} to {1}", path, perms), e);
			}
			throw new IOException(e);
		}
	}
	
	/**
	 * Sets the modified and/or creation time of the provided path.
	 * 
	 * @param path the path of the file to set
	 * @param lastModifiedTime the last modified time, if desired
	 * @param createTime the creation time, if desired
	 * @throws IOException if there is a database problem setting the metadata
	 */
	public static void setTimes(NSFPath path, FileTime lastModifiedTime, FileTime createTime) throws IOException {
		try {
			NSFPathUtil.runWithDocument(path, doc -> {
				if(lastModifiedTime != null) {
					doc.replaceItemValue(ITEM_MODIFIED, lastModifiedTime.toInstant());
				}
				if(createTime != null) {
					doc.replaceItemValue(ITEM_CREATED, createTime.toInstant());
				}
				
				doc.save();
				NSFPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception setting timestamps of {0} to {1}, {2}", path, lastModifiedTime, createTime), e);
			}
			throw new IOException(e);
		}
	}
	
	/**
	 * Lists the names of any user-defined attributes on the provided path
	 * 
	 * @param path the path to check
	 * @return a {@link List} of attribute names
	 * @throws IOException if there is a DB problem reading the names
	 */
	public static List<String> listUserDefinedAttributes(NSFPath path) throws IOException {
		try {
			String cacheId = "userAttrs-" + path; //$NON-NLS-1$
			return NSFPathUtil.callWithDocument(path, cacheId, doc ->
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
	
	/**
	 * Writes the provided data to the named user-defined attribute in the path
	 * 
	 * @param path the path of the file to amend
	 * @param name the name of the user-defined attribute
	 * @param src a buffer containing the data
	 * @return the number of bytes written
	 * @throws IOException if there is a DB problem writing the data
	 */
	public static int writeUserDefinedAttribute(NSFPath path, String name, ByteBuffer src) throws IOException {
		try {
			return NSFPathUtil.callWithDocument(path, null, doc -> {
				String itemName = PREFIX_USERITEM + name;
				byte[] data = src.array();
				UserData userData = doc.getParentDatabase().getParentDominoClient().createUserData(DATATYPE_NAME, data);
				doc.replaceItemValue(itemName, userData);
				doc.computeWithForm(true, null);
				NSFPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
				return data.length;
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception setting user-defined attribute {0} on {1}", name, path), e);
			}
			throw new IOException(e);
		}
	}

	/**
	 * Deletes the named user-defined attribute from the provided path
	 * 
	 * @param path the path of the file to adjust
	 * @param name the name of the user-defined attribute
	 * @throws IOException if there is a DB problem deleting the data
	 */
	public static void deleteUserDefinedAttribute(NSFPath path, String name) throws IOException {
		try {
			NSFPathUtil.runWithDocument(path, doc -> {
				String itemName = PREFIX_USERITEM + name;
				if(doc.hasItem(itemName)) {
					doc.removeItem(itemName);
					doc.computeWithForm(true, null);
					doc.save();
					NSFPathUtil.invalidateDatabaseCache(doc.getParentDatabase());
				}
			});
		} catch(RuntimeException e) {
			if(log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, MessageFormat.format("Encountered exception deleting user-defined attribute {0} on {1}", name, path), e);
			}
			throw new IOException(e);
		}
	}
	
	/**
	 * Reads the provided user-defined attribute from the provided path.
	 * 
	 * @param path the path of the file to read
	 * @param name the name of the attribute to read
	 * @return the attribute data as a byte array
	 * @throws IOException if there is a DB problem reading the data
	 */
	public static byte[] getUserDefinedAttribute(NSFPath path, String name) throws IOException {
		try {
			String cacheId = "userAttrVal-" + path + name; //$NON-NLS-1$
			return NSFPathUtil.callWithDocument(path, cacheId, doc -> {
				String itemName = PREFIX_USERITEM + name;
				return doc.getFirstItem(itemName)
					.map(item -> {
						switch(item.getType()) {
						case TYPE_TEXT:
						case TYPE_TEXT_LIST:
							return item.get(String.class, "").getBytes(StandardCharsets.UTF_8);
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

	/**
	 * Retrieves the document for the provided path, creating a new in-memory document
	 * if needed.
	 * 
	 * @param path the path to find the document for
	 * @param database the database housing the document
	 * @return a document representing the note
	 */
	public static Document getDocument(NSFPath path, Database database) {
		DominoCollection view = database.openCollection(VIEW_FILESBYPATH)
			.orElseThrow(() -> new IllegalStateException(MessageFormat.format("Unable to open view \"{0}\" in database \"{1}\"", VIEW_FILESBYPATH, database.getRelativeFilePath())));
		view.refresh();
		return view.query()
			.selectByKey(path.toAbsolutePath().toString(), true)
			.firstId()
			.flatMap(database::getDocumentById)
			.orElseGet(() -> {
				Document doc = database.createDocument();
				doc.replaceItemValue(ITEM_PARENT, path.getParent().toAbsolutePath().toString());
				doc.replaceItemValue(NotesConstants.ITEM_META_TITLE, path.getFileName().toString());
				return doc;
			});
	}
}
