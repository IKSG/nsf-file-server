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
package org.openntf.nsffile.fs.abstractnsf.attribute;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.openntf.nsffile.core.NotesPrincipal;

public class RootFileAttributes implements PosixFileAttributes {
	
	private final Instant modified;
	private final Instant created;
	
	public RootFileAttributes(Instant modified, Instant created) {
		this.modified = modified;
		this.created = created;
	}

	@Override
	public UserPrincipal owner() {
		return new NotesPrincipal("CN=root"); //$NON-NLS-1$
	}

	@Override
	public GroupPrincipal group() {
		return new NotesPrincipal("CN=wheel"); //$NON-NLS-1$
	}

	@Override
	public Set<PosixFilePermission> permissions() {
		return EnumSet.allOf(PosixFilePermission.class);
	}

	@Override
	public FileTime lastModifiedTime() {
		return FileTime.from(modified);
	}

	@Override
	public FileTime lastAccessTime() {
		return FileTime.from(Instant.now());
	}

	@Override
	public FileTime creationTime() {
		return FileTime.from(created);
	}

	@Override
	public boolean isRegularFile() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return true;
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
		return 0;
	}

	@Override
	public Object fileKey() {
		return null;
	}

}
