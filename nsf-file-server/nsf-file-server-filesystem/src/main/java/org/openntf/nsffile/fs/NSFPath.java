package org.openntf.nsffile.fs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

import org.apache.sshd.common.file.util.BasePath;

public class NSFPath extends BasePath<NSFPath, NSFFileSystem> {
	public NSFPath(NSFFileSystem fileSystem, String root, List<String> names) {
		super(fileSystem, root, names);
	}

    @Override
    public NSFPath toRealPath(LinkOption... options) throws IOException {
        // TODO: handle links
    	NSFPath absolute = toAbsolutePath();
        FileSystem fs = getFileSystem();
        FileSystemProvider provider = fs.provider();
        provider.checkAccess(absolute);
        return absolute;
    }

}
