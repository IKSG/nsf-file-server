package org.openntf.nsffile.config;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.util.NSFPathUtil;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
@ApplicationScoped
public class NSFConfiguration implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String ENV_DBPATH = "SFTPNSFPath";
	public static final String DEFAULT_DBPATH = "filestore.nsf";
	
	@Inject @ConfigProperty(name=ENV_DBPATH, defaultValue=DEFAULT_DBPATH)
	String nsfPath;
	
	public String getNsfPath() {
		return nsfPath;
	}
	
	public FileSystem getFileSystem(String username) throws IOException {
		try {
			URI uri = NSFPathUtil.toFileSystemURI(username, nsfPath);
			return NSFFileSystemProvider.instance.getOrCreateFileSystem(uri, Collections.emptyMap());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
