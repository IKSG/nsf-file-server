package org.openntf.nsffile.fs.abstractnsf.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.hcl.domino.data.Document.IAttachmentProducer;

public class PathAttachmentProducer implements IAttachmentProducer {
	private final Path path;
	
	public PathAttachmentProducer(Path path) {
		this.path = path;
	}

	@Override
	public long getSizeEstimation() {
		try {
			return Files.size(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void produceAttachment(OutputStream out) throws IOException {
		Files.copy(path, out);
	}

}
