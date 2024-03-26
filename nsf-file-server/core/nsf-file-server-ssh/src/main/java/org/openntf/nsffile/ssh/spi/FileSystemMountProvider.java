package org.openntf.nsffile.ssh.spi;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Map;

/**
 * Service interface to declare a filesystem mount type as configured
 * in the NSF.
 */
public interface FileSystemMountProvider {
	String getName();
	
	FileSystem createFileSystem(String dataSource, Map<String, ?> env) throws IOException;
}
