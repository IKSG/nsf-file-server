package org.openntf.nsffile.core.provider;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.sshd.common.file.root.RootedFileSystemProvider;
import org.openntf.nsffile.core.spi.FileSystemMountProvider;

public class LocalFileSystemMountProvider implements FileSystemMountProvider {
	private final RootedFileSystemProvider provider = new RootedFileSystemProvider();

	@Override
	public String getName() {
		return "filesystem"; //$NON-NLS-1$
	}

	@Override
	public FileSystem createFileSystem(String dataSource, Map<String, Object> env) throws IOException {
		Path path = Paths.get(dataSource);
		
		try {
			return provider.newFileSystem(path, env);
		} catch(FileSystemAlreadyExistsException e) {
			return provider.getFileSystem(path.toUri());
		}
	}

}
