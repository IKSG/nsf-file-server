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
package org.openntf.nsffile.core.fs;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.apache.sshd.sftp.server.DefaultGroupPrincipal;
import org.apache.sshd.sftp.server.DefaultUserPrincipal;

public class RootFileAttributes implements PosixFileAttributes {
	public static final RootFileAttributes instance = new RootFileAttributes();

	@Override
	public FileTime lastModifiedTime() {
		return FileTime.from(Instant.now());
	}

	@Override
	public FileTime lastAccessTime() {
		return FileTime.from(Instant.now());
	}

	@Override
	public FileTime creationTime() {
		return FileTime.from(Instant.EPOCH);
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
		return instance;
	}

	@Override
	public UserPrincipal owner() {
		return new DefaultUserPrincipal("root"); //$NON-NLS-1$
	}

	@Override
	public GroupPrincipal group() {
		return new DefaultGroupPrincipal("wheel"); //$NON-NLS-1$
	}

	@Override
	public Set<PosixFilePermission> permissions() {
		return EnumSet.of(
			PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
			PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
			PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE
		);
	}

}
