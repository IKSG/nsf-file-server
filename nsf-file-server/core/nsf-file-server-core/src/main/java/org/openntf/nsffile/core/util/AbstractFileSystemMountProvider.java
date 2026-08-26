package org.openntf.nsffile.core.util;

import org.openntf.nsffile.core.spi.FileSystemMountProvider;

public abstract class AbstractFileSystemMountProvider implements FileSystemMountProvider {
	public static final String KEY_USERNAME = "username"; //$NON-NLS-1$
	
	private final String name;
	
	public AbstractFileSystemMountProvider(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

}
