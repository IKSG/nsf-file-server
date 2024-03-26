package org.openntf.nsffile.domino;

import java.nio.file.Path;

import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

public class NSFHostKeyProvider extends SimpleGeneratorHostKeyProvider {

	public NSFHostKeyProvider(Path path) {
		// TODO change to use the NSF
		super(path);
	}
	
}
