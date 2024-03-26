package org.openntf.nsffile.ssh.provider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.sshd.common.file.root.RootedFileSystemProvider;
import org.openntf.nsffile.ssh.spi.FileSystemMountProvider;

public class URIMountProvider implements FileSystemMountProvider {

	@Override
	public String getName() {
		return "uri"; //$NON-NLS-1$
	}

	@Override
	public FileSystem createFileSystem(String dataSource, Map<String, ?> env) throws IOException {
		RootedFileSystemProvider provider = new RootedFileSystemProvider();
		URI uri = URI.create(dataSource);
		Path path = Paths.get(uri);
		return provider.newFileSystem(path, env);
	}

}
