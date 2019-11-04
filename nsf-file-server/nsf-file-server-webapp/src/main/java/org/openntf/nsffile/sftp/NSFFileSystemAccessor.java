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
package org.openntf.nsffile.sftp;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import java.util.Set;

import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.DirectoryHandle;
import org.apache.sshd.server.subsystem.sftp.FileHandle;
import org.apache.sshd.server.subsystem.sftp.SftpEventListenerManager;
import org.apache.sshd.server.subsystem.sftp.SftpFileSystemAccessor;

public class NSFFileSystemAccessor implements SftpFileSystemAccessor {
	private final String nsfPath;
	
	public NSFFileSystemAccessor(String nsfPath) {
		this.nsfPath = Objects.requireNonNull(nsfPath, "nsfPath cannot be null");
	}

	@Override
	public SeekableByteChannel openFile(ServerSession session, SftpEventListenerManager subsystem,
			FileHandle fileHandle, Path file, String handle, Set<? extends OpenOption> options,
			FileAttribute<?>... attrs) throws IOException {
		System.out.println("open file, user=" + session.getUsername() + ", path=" + file + ", handle=" + handle + ", options=" + options + ", attrs=" + attrs);
		
		return SftpFileSystemAccessor.super.openFile(session, subsystem, fileHandle, file, handle, options, attrs);
	}

	@Override
	public FileLock tryLock(ServerSession session, SftpEventListenerManager subsystem, FileHandle fileHandle, Path file,
			String handle, Channel channel, long position, long size, boolean shared) throws IOException {
		System.out.println("tryLock, user=" + session.getUsername() + ", path=" + file);
		return SftpFileSystemAccessor.super.tryLock(session, subsystem, fileHandle, file, handle, channel, position, size,
				shared);
	}

	@Override
	public void syncFileData(ServerSession session, SftpEventListenerManager subsystem, FileHandle fileHandle,
			Path file, String handle, Channel channel) throws IOException {
		System.out.println("syncFileData, user=" + session.getUsername() + ", path=" + file);
		SftpFileSystemAccessor.super.syncFileData(session, subsystem, fileHandle, file, handle, channel);
	}

	@Override
	public void closeFile(ServerSession session, SftpEventListenerManager subsystem, FileHandle fileHandle, Path file,
			String handle, Channel channel, Set<? extends OpenOption> options) throws IOException {
		System.out.println("closeFile, user=" + session.getUsername() + ", path=" + file);
		SftpFileSystemAccessor.super.closeFile(session, subsystem, fileHandle, file, handle, channel, options);
	}

	@Override
	public DirectoryStream<Path> openDirectory(ServerSession session, SftpEventListenerManager subsystem,
			DirectoryHandle dirHandle, Path dir, String handle) throws IOException {
		System.out.println("openDirectory, user=" + session.getUsername() + ", path=" + dir + ", handle=" + handle);
		
		return SftpFileSystemAccessor.super.openDirectory(session, subsystem, dirHandle, dir, handle);
	}

	@Override
	public void closeDirectory(ServerSession session, SftpEventListenerManager subsystem, DirectoryHandle dirHandle,
			Path dir, String handle, DirectoryStream<Path> ds) throws IOException {
		System.out.println("closeDirectory, user=" + session.getUsername() + ", path=" + dir);
		SftpFileSystemAccessor.super.closeDirectory(session, subsystem, dirHandle, dir, handle, ds);
	}
	
	
}
