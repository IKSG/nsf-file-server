/**
 * Copyright (c) 2019-2024 Jesse Gallagher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openntf.nsffile.ssh;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hcl.domino.DominoClient;
import com.hcl.domino.admin.ServerStatistics;
import com.hcl.domino.mq.MessageQueue;
import com.hcl.domino.server.RunJavaAddin;
import com.hcl.domino.server.ServerStatusLine;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.ServerBuilder;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.RejectAllPasswordAuthenticator;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.openntf.nsffile.core.config.DominoNSFConfiguration;
import org.openntf.nsffile.core.util.NotesThreadFactory;
import org.openntf.nsffile.ssh.auth.NotesPasswordAuthenticator;
import org.openntf.nsffile.ssh.auth.NotesPublicKeyAuthenticator;

/**
 * Frontend-independent manager for running the SSH/SFTP server.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
@SuppressWarnings("nls")
public class SshServerAddin extends RunJavaAddin {
	public static final Logger log = Logger.getLogger(SshServerAddin.class.getPackage().getName());

	public static final String ADDIN_NAME = "SFTP Server";
	public static final String QUEUE_NAME = "sftp";

	public static final String STAT_FACILITY = "SFTP";
	public static final String STAT_SESSIONS = "CurrentSessions";
	private static final String[] STAT_NAMES = { STAT_SESSIONS };

	private final int port;
	private final KeyPairProvider keyPairProvider;
	private final FileSystemFactory fileSystemFactory;
	private final CommandFactory commandFactory;
	private DominoClient client;

	public SshServerAddin(int port, KeyPairProvider keyPairProvider, FileSystemFactory fileSystemFactory,
			CommandFactory commandFactory) {
		super(ADDIN_NAME, QUEUE_NAME);

		this.port = port;
		this.keyPairProvider = keyPairProvider;
		this.fileSystemFactory = fileSystemFactory;
		this.commandFactory = commandFactory;
	}

	@Override
	protected void runAddin(DominoClient client, ServerStatusLine statusLine, MessageQueue mq) {
		if (log.isLoggable(Level.INFO)) {
			log.info(getClass().getSimpleName() + ": Startup");
			log.info(getClass().getSimpleName() + ": Using port " + port);
			log.info(getClass().getSimpleName() + ": Using key provider " + keyPairProvider);
		}
		this.client = client;
		ServerStatistics stats = client.getServerStatistics();

		ServerBuilder builder = ServerBuilder.builder()
			.fileSystemFactory(fileSystemFactory)
			.publickeyAuthenticator(new NotesPublicKeyAuthenticator());
		try (SshServer server = builder.build()) {
			server.setPort(port);
			server.setKeyPairProvider(keyPairProvider);

			if (DominoNSFConfiguration.instance.isAllowPasswordAuth()) {
				server.setPasswordAuthenticator(new NotesPasswordAuthenticator());
			} else {
				server.setPasswordAuthenticator(RejectAllPasswordAuthenticator.INSTANCE);
			}

			SftpSubsystemFactory sftp = new SftpSubsystemFactory.Builder()
					.build();
			server.setSubsystemFactories(Collections.singletonList(sftp));

			server.setCommandFactory(commandFactory);
			server.setScheduledExecutorService(NotesThreadFactory.scheduler);

			server.start();

			statusLine.setLine(MessageFormat.format("Listen for connect requests on TCP Port:{0}", Integer.toString(port)));

			if (log.isLoggable(Level.INFO)) {
				log.info(MessageFormat.format("Initialized SFTP server on port {0}", Integer.toString(port)));
			}
			log("Started");

			NotesThreadFactory.scheduler.scheduleWithFixedDelay(() -> {
				stats.updateStatistic(STAT_FACILITY, STAT_SESSIONS, EnumSet.of(ServerStatistics.Flag.UNIQUE),
						server.getActiveSessions().size());
			}, 0, 10, TimeUnit.SECONDS);

			while (!mq.isQuitPending()) {
				Optional<String> message;
				while ((message = mq.get(5, TimeUnit.SECONDS)) != null) {
					message.ifPresent(msg -> {
						// if we got a message in the queue, process it
						// TODO actually implement some messages
					});
				}
			}
		} catch (IOException e) {
			if (log.isLoggable(Level.SEVERE)) {
				log.log(Level.SEVERE, "Encountered exception running SFTP server", e);
			}
		} catch (InterruptedException e) {
			// Fine - meant to quit
		} finally {
			Arrays.stream(STAT_NAMES).forEach(stat -> stats.deleteStatistic(STAT_FACILITY, stat));
		}

		log("Shutdown");
	}

	private void log(String s) {
		if (s == null) {
			return;
		}
		String msg = format(s);
		if (msg.isEmpty()) {
			return;
		}

		if(this.client == null) {
			System.out.println(String.format("%s: %s", ADDIN_NAME, msg));
		} else {
			this.client.getServerAdmin().logMessage(String.format("%s: %s", ADDIN_NAME, msg));
		}
	}

	private static String format(String s) {
		if (s.isEmpty()) {
			return s;
		}
		int lastGood = s.length();
		for (lastGood = s.length() - 1; lastGood >= 0; lastGood--) {
			if (!Character.isWhitespace(s.charAt(lastGood))) {
				return s.substring(0, lastGood + 1);
			}
		}
		return "";
	}
}
