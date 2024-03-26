package org.openntf.nsffile.commons.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CompositeDirectoryStream implements DirectoryStream<Path> {
	private final List<Path> paths;
	
	public CompositeDirectoryStream(CompositeFileSystem fileSystem) {
		paths = fileSystem.getFileSystems().keySet()
			.stream()
			.map(mount -> fileSystem.getPath("/", mount)) //$NON-NLS-1$
			.collect(Collectors.toList());
	}

	@Override
	public Iterator<Path> iterator() {
		return paths.iterator();
	}

	@Override
	public void close() throws IOException {
		
	}

}
