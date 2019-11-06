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
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.sshd.server.subsystem.sftp.DefaultGroupPrincipal;
import org.apache.sshd.server.subsystem.sftp.DefaultUserPrincipal;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
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
	
	public NSFFileAttributes(NSFFileSystemProvider provider, NSFPath path) {
		try {
			NSFPathUtil.runWithDocument(path, doc -> {
				try {
					if(!doc.isNewNote()) {
						@SuppressWarnings("unchecked")
						List<String> updatedBy = doc.getItemValue(NotesConstants.FIELD_UPDATED_BY);
						if(updatedBy != null && !updatedBy.isEmpty()) {
							owner = NSFPathUtil.shortCn(updatedBy.get(0));
						} else {
							owner = NSFPathUtil.shortCn(doc.getParentDatabase().getParent().getEffectiveUserName());
						}
						group = "wheel"; // TODO implement //$NON-NLS-1$
						String form = doc.getItemValueString(NotesConstants.FIELD_FORM);
						if(StringUtil.isNotEmpty(form)) {
							type = Type.valueOf(form);
						} else {
							type = null;
						}
						DateTime mod = doc.getLastModified();
						if(mod != null) {
							lastModified = FileTime.fromMillis(mod.toJavaDate().getTime());
						} else {
							lastModified = FileTime.fromMillis(System.currentTimeMillis());
						}
						DateTime acc = doc.getLastAccessed();
						if(acc != null) {
							lastAccessed = FileTime.fromMillis(acc.toJavaDate().getTime());
						} else {
							lastAccessed = FileTime.fromMillis(System.currentTimeMillis());
						}
						created = FileTime.fromMillis(doc.getCreated().toJavaDate().getTime());
						
						// TODO check attachment size
						if(doc.hasItem(ITEM_FILE)) {
							RichTextItem item = (RichTextItem)doc.getFirstItem(ITEM_FILE);
							@SuppressWarnings("unchecked")
							List<EmbeddedObject> eos = item.getEmbeddedObjects();
							if(!eos.isEmpty()) {
								size = eos.get(0).getFileSize();
							}
						}
					} else {
						owner = "root"; //$NON-NLS-1$
						group = "wheel"; //$NON-NLS-1$
						lastModified = FileTime.from(Instant.EPOCH);
						lastAccessed = FileTime.from(Instant.EPOCH);
						created = FileTime.from(Instant.EPOCH);
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
		return EnumSet.allOf(PosixFilePermission.class);
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
