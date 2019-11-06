/**
 * Copyright © 2019 Jesse Gallagher
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
package org.openntf.nsffile.fs.attribute;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.Vector;

import org.apache.sshd.server.subsystem.sftp.DefaultGroupPrincipal;
import org.apache.sshd.server.subsystem.sftp.DefaultUserPrincipal;
import org.openntf.nsffile.fs.NSFPath;
import org.openntf.nsffile.fs.util.NSFPathUtil;

import com.ibm.commons.util.StringUtil;
import com.ibm.designer.domino.napi.NotesConstants;

import lotus.domino.DateTime;
import lotus.domino.EmbeddedObject;
import lotus.domino.RichTextItem;

import static org.openntf.nsffile.fs.NSFFileSystemConstants.*;

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
	private FileTime lastModified;
	private FileTime lastAccessed;
	private FileTime created;
	private long size;
	private Set<PosixFilePermission> permissions;
	
	public NSFFileAttributes(NSFPath path) {
		try {
			NSFPathUtil.runWithDocument(path, doc -> {
				try {
					if(!doc.isNewNote()) {
						owner = NSFPathUtil.shortCn(doc.getItemValueString(ITEM_OWNER));
						group = NSFPathUtil.shortCn(doc.getItemValueString(ITEM_GROUP));
						
						String form = doc.getItemValueString(NotesConstants.FIELD_FORM);
						if(StringUtil.isNotEmpty(form)) {
							type = Type.valueOf(form);
						} else {
							type = null;
						}
						if(doc.hasItem(ITEM_MODIFIED)) {
							@SuppressWarnings("unchecked")
							Vector<DateTime> mod = (Vector<DateTime>)doc.getItemValueDateTimeArray(ITEM_MODIFIED);
							try {
								lastModified = FileTime.fromMillis(mod.get(0).toJavaDate().getTime());
							} finally {
								doc.recycle(mod);;
							}
						} else {
							lastModified = FileTime.from(Instant.now());
						}
						DateTime acc = doc.getLastAccessed();
						if(acc != null) {
							try {
								lastAccessed = FileTime.fromMillis(acc.toJavaDate().getTime());
							} finally {
								acc.recycle();
							}
						} else {
							lastAccessed = FileTime.from(Instant.now());
						}
						if(doc.hasItem(ITEM_CREATED)) {
							@SuppressWarnings("unchecked")
							Vector<DateTime> c = (Vector<DateTime>)doc.getItemValueDateTimeArray(ITEM_CREATED);
							try {
								created = FileTime.fromMillis(c.get(0).toJavaDate().getTime());
							} finally {
								doc.recycle(c);
							}
						} else {
							created = FileTime.from(Instant.now());
						}
						
						// TODO check attachment size
						if(doc.hasItem(ITEM_FILE)) {
							RichTextItem item = (RichTextItem)doc.getFirstItem(ITEM_FILE);
							try {
								@SuppressWarnings("unchecked")
								Vector<EmbeddedObject> eos = item.getEmbeddedObjects();
								try {
									if(!eos.isEmpty()) {
										size = eos.get(0).getFileSize();
									}
								} finally {
									item.recycle(eos);
								}
							} finally {
								item.recycle();
							}
						}
						
						permissions = PosixFilePermissions.fromString(doc.getItemValueString(ITEM_PERMISSIONS));
					} else {
						owner = "root"; //$NON-NLS-1$
						group = "wheel"; //$NON-NLS-1$
						lastModified = FileTime.from(Instant.EPOCH);
						lastAccessed = FileTime.from(Instant.EPOCH);
						created = FileTime.from(Instant.EPOCH);
						permissions = EnumSet.allOf(PosixFilePermission.class);
					}
				} catch(Throwable t) {
					t.printStackTrace();
					throw t;
				}
			});
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
		return permissions;
	}

	@Override
	public FileTime lastModifiedTime() {
		return lastModified;
	}

	@Override
	public FileTime lastAccessTime() {
		return lastAccessed;
	}

	@Override
	public FileTime creationTime() {
		return created;
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
