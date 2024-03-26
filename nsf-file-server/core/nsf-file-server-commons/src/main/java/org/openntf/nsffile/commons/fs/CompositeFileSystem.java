package org.openntf.nsffile.commons.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.sshd.common.file.util.BaseFileSystem;
import org.apache.sshd.sftp.client.fs.SftpFileSystem.DefaultUserPrincipalLookupService;

public class CompositeFileSystem extends BaseFileSystem<CompositePath> {
	private static final Logger log = Logger.getLogger(CompositeFileSystem.class.getPackage().getName());
	
	private final Map<String, FileSystem> fileSystems;
	private boolean closed = false;
	private final List<FileStore> fileStores;
	
	public CompositeFileSystem(FileSystemProvider fileSystemProvider, Map<String, FileSystem> fileSystems) {
		super(fileSystemProvider);
		this.fileSystems = fileSystems;
		this.fileStores = Arrays.asList(new CompositeFileStore(this));
	}
	
	public Map<String, FileSystem> getFileSystems() {
		return fileSystems;
	}

	@Override
	protected CompositePath create(String root, List<String> names) {
		if(log.isLoggable(Level.FINEST)) {
			log.finest(MessageFormat.format("Creating path for root \"{0}\" and names \"{1}\"", root, names));
		}
		List<String> bits = names == null || names.isEmpty() ? Arrays.asList("") : names; //$NON-NLS-1$
		return new CompositePath(this, root, bits);
	}
	
	@Override
	public Iterable<FileStore> getFileStores() {
		return this.fileStores;
	}

	@Override
	public void close() throws IOException {
		for(FileSystem fileSystem : fileSystems.values()) {
			fileSystem.close();
		}
		this.closed = true;
	}

	@Override
	public boolean isOpen() {
		return !this.closed;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return this.fileSystems.values().stream()
			.flatMap(fs -> fs.supportedFileAttributeViews().stream())
			.collect(Collectors.toSet());
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		return DefaultUserPrincipalLookupService.INSTANCE;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileStores, fileSystems);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CompositeFileSystem other = (CompositeFileSystem) obj;
		return Objects.equals(fileStores, other.fileStores) && Objects.equals(fileSystems, other.fileSystems);
	}

}
