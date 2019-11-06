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

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Date;
import java.util.Set;

import org.openntf.nsffile.fs.NSFPath;
import org.openntf.nsffile.fs.util.NSFPathUtil;

import lotus.domino.DateTime;
import lotus.domino.Session;

import static org.openntf.nsffile.fs.NSFFileSystemConstants.*;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NSFPosixFileAttributeView implements PosixFileAttributeView, BasicFileAttributeView, FileOwnerAttributeView {
	
	private final NSFPath path;
	
	public NSFPosixFileAttributeView(NSFPath path, LinkOption... options) {
		this.path = path;
    }

	@Override
	public String name() {
		return "posix"; //$NON-NLS-1$
	}

	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
		NSFPathUtil.runWithDocument(this.path, doc -> {
			Session session = doc.getParentDatabase().getParent();
			if(lastModifiedTime != null) {
				DateTime mod = session.createDateTime(new Date(lastModifiedTime.toMillis()));
				doc.replaceItemValue(ITEM_MODIFIED, mod);
			}
			if(createTime != null) {
				DateTime created = session.createDateTime(new Date(createTime.toMillis()));
				doc.replaceItemValue(ITEM_CREATED, created);
			}
			
			doc.save();
		});
	}

	@Override
	public UserPrincipal getOwner() throws IOException {
		return this.readAttributes().owner();
	}

	@Override
	public void setOwner(UserPrincipal owner) throws IOException {
		NSFPathUtil.runWithDocument(this.path, doc -> {
			doc.replaceItemValue(ITEM_OWNER, owner.getName());
			doc.save();
		});
	}

	@Override
	public synchronized PosixFileAttributes readAttributes() throws IOException {
		// TODO cache?
		return new NSFFileAttributes(path);
	}

	@Override
	public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
		NSFPathUtil.runWithDocument(this.path, doc -> {
			doc.replaceItemValue(ITEM_PERMISSIONS, PosixFilePermissions.toString(perms));
			doc.save();
		});
	}

	@Override
	public void setGroup(GroupPrincipal group) throws IOException {
		NSFPathUtil.runWithDocument(this.path, doc -> {
			doc.replaceItemValue(ITEM_GROUP, group.getName());
			doc.save();
		});
	}
	
}
