package org.openntf.nsffile.commons.fs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

import org.apache.sshd.common.file.util.BasePath;

public class CompositePath extends BasePath<CompositePath, CompositeFileSystem> {
	
	public CompositePath(CompositeFileSystem fileSystem, String root, List<String> names) {
		super(fileSystem, root, names);
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		// TODO: handle links
		CompositePath absolute = toAbsolutePath();
        FileSystem fs = getFileSystem();
        FileSystemProvider provider = fs.provider();
        provider.checkAccess(absolute);
        return absolute;
	}


}
