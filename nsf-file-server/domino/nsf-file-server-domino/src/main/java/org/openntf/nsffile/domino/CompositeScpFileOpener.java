package org.openntf.nsffile.domino;

import org.openntf.nsffile.domino.config.DominoNSFConfiguration;
import org.openntf.nsffile.ssh.scp.NSFScpFileOpener;

public class CompositeScpFileOpener extends NSFScpFileOpener {

	public CompositeScpFileOpener() {
		// TODO make actually composite
		super(DominoNSFConfiguration.instance.getConfigNsfPath());
	}


}
