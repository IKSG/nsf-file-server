package org.openntf.nsffile.ssh.scp;

import java.io.IOException;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

public class DummyShellFactory implements ShellFactory {

	@Override
	public Command createShell(ChannelSession channel) throws IOException {
		throw new UnsupportedOperationException();
	}

}
