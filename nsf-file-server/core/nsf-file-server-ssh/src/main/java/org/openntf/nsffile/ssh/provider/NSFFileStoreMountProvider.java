package org.openntf.nsffile.ssh.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import com.ibm.commons.util.StringUtil;

import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.fs.util.NSFPathUtil;
import org.openntf.nsffile.ssh.spi.FileSystemMountProvider;

public class NSFFileStoreMountProvider implements FileSystemMountProvider {
	public static final String KEY_USERNAME = "username"; //$NON-NLS-1$

	@Override
	public String getName() {
		return NSFFileSystemProvider.SCHEME;
	}

	@Override
	public FileSystem createFileSystem(String dataSource, Map<String, ?> env) throws IOException {
		String username = (String)env.get(KEY_USERNAME);
		if(StringUtil.isEmpty(username)) {
			throw new IllegalArgumentException(MessageFormat.format("env map must contain a {0} key", KEY_USERNAME));
		}
		try {
			URI uri = NSFPathUtil.toFileSystemURI(username, dataSource);
			return NSFFileSystemProvider.instance.getOrCreateFileSystem(uri, Collections.emptyMap());
		} catch (URISyntaxException e) {
			throw new IOException(MessageFormat.format("Unable to build URI for data source {0}", dataSource), e);
		}
	}

}
