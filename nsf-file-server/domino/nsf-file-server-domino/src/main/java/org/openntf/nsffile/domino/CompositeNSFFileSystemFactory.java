package org.openntf.nsffile.domino;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.SessionContext;
import org.openntf.nsffile.domino.config.DominoNSFConfiguration;

public class CompositeNSFFileSystemFactory implements FileSystemFactory {
	@Override
	public Path getUserHomeDir(SessionContext session) throws IOException {
		return null;
	}

	@Override
	public FileSystem createFileSystem(SessionContext session) throws IOException {
		return DominoNSFConfiguration.instance.buildFileSystem(session.getUsername());
	}
}
