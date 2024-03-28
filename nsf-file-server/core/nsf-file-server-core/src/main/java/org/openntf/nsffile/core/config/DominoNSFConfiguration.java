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
package org.openntf.nsffile.core.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hcl.domino.DominoClient;
import com.hcl.domino.data.CollectionEntry;
import com.hcl.domino.data.CollectionSearchQuery.CollectionEntryProcessor;
import com.hcl.domino.data.Database;
import com.hcl.domino.data.Database.Action;
import com.hcl.domino.data.Document;
import com.hcl.domino.data.DominoCollection;
import com.hcl.domino.naming.Names;
import com.ibm.commons.util.StringUtil;
import com.ibm.commons.util.io.json.JsonException;
import com.ibm.commons.util.io.json.JsonJavaFactory;
import com.ibm.commons.util.io.json.JsonParser;

import org.openntf.nsffile.core.fs.CompositeFileSystem;
import org.openntf.nsffile.core.fs.CompositeFileSystemProvider;
import org.openntf.nsffile.core.spi.FileSystemMountProvider;
import org.openntf.nsffile.core.util.NSFFileUtil;
import org.openntf.nsffile.core.util.NotesThreadFactory;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public enum DominoNSFConfiguration {
	instance;
	
	private static final Logger log = Logger.getLogger(DominoNSFConfiguration.class.getPackage().getName());
	
	public static final String ENV_DBPATH = "SFTPConfigPath"; //$NON-NLS-1$
	public static final String DEFAULT_DBPATH = "fileserverconfig.nsf"; //$NON-NLS-1$
	public static final int DEFAULT_PORT = 9022;
	public static final String VIEW_MOUNTS = "Mounts"; //$NON-NLS-1$
	public static final int COL_INDEX_PATH = 0;
	public static final int COL_INDEX_TYPE = 1;
	public static final int COL_INDEX_DATASOURCE = 2;
	public static final int COL_INDEX_SERVERS = 3;
	public static final int COL_INDEX_ENV = 4;
	
	public static final String ITEM_SERVERNAME = "ServerName"; //$NON-NLS-1$
	
	public static final String VIEW_CONFIG = "ServerConfigurations"; //$NON-NLS-1$
	public static final String FORM_CONFIG = "ServerConfiguration"; //$NON-NLS-1$
	public static final String ITEM_ENABLED = "SSHEnabled"; //$NON-NLS-1$
	public static final String ITEM_PORT = "SSHPort"; //$NON-NLS-1$
	public static final String ITEM_PASSWORDAUTH = "SSHAllowPasswordAuth"; //$NON-NLS-1$
	
	public static final String VIEW_SSHKEYPAIRS = "ServerSSHKeyPairs"; //$NON-NLS-1$
	public static final String FORM_SSHKEYPAIR = "ServerSSHKeyPair"; //$NON-NLS-1$
	public static final String ITEM_PUBKEY = "PublicKey"; //$NON-NLS-1$
	public static final String ITEM_PRIVATEKEY = "PrivateKey"; //$NON-NLS-1$

	private String nsfPath;
	
	public String getConfigNsfPath() {
		if(this.nsfPath == null) {
			this.nsfPath = NotesThreadFactory.callJnx(client -> {
				String envProperty = client.getDominoRuntime().getPropertyString(ENV_DBPATH);
				if(StringUtil.isEmpty(envProperty)) {
					envProperty = DEFAULT_DBPATH;
				}
				return envProperty;
			});
		}
		return nsfPath;
	}
	
	public boolean isEnabled() {
		return NotesThreadFactory.callJnx(client -> {
			return getServerDoc(client)
				.map(doc -> !"N".equals(doc.getAsText(ITEM_ENABLED, ' '))) //$NON-NLS-1$
				.orElse(true);
		});
	}
	
	public int getPort() {
		return NotesThreadFactory.callJnx(client -> {
			int port = getServerDoc(client)
				.map(doc -> doc.get(ITEM_PORT, int.class, 0))
				.orElse(0);
			return port == 0 ? DEFAULT_PORT : port;
		});
	}
	
	public boolean isAllowPasswordAuth() {
		return NotesThreadFactory.callJnx(client -> {
			return getServerDoc(client)
				.map(doc -> "Y".equals(doc.getAsText(ITEM_PASSWORDAUTH, ' '))) //$NON-NLS-1$
				.orElse(false);
		});
	}
	
	public CompositeFileSystem buildFileSystem(String username) {
		// Read the view to create filesystems for each entry
		Map<String, FileSystem> fileSystems = NotesThreadFactory.callJnx(client -> {
			try {
				List<String> names = Names.buildNamesList(client, client.getIDUserName()).toList();
				
				List<FileSystemMountProvider> providers = NSFFileUtil.findExtensions(FileSystemMountProvider.class);
				if(log.isLoggable(Level.FINEST)) {
					log.finest(MessageFormat.format("Found providers: {0}", providers));
				}
				
				Database configNsf = client.openDatabase(getConfigNsfPath());
				DominoCollection mounts = configNsf.openCollection(VIEW_MOUNTS)
						.orElseThrow(() -> new IllegalStateException(MessageFormat.format("Unable to open view \"{0}\" in database \"{1}\"", VIEW_MOUNTS, getConfigNsfPath())));
				return mounts.query()
					.readColumnValues()
					.build(0, Integer.MAX_VALUE, new CollectionEntryProcessor<Map<String, FileSystem>>() {
						
					@Override
					public Map<String, FileSystem> start() {
						return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
					}

					@Override
					public Action entryRead(Map<String, FileSystem> result, CollectionEntry entry) {
						List<String> servers = entry.getAsList(DominoNSFConfiguration.COL_INDEX_SERVERS, String.class, Collections.emptyList());
						if(servers.stream().anyMatch(s -> names.contains(s))) {
							if(log.isLoggable(Level.FINEST)) {
								log.finest("Acceptable for current server");
							}
							
							String path = (String)entry.get(DominoNSFConfiguration.COL_INDEX_PATH, String.class, null);
							String type = (String)entry.get(DominoNSFConfiguration.COL_INDEX_TYPE, String.class, null);
							String dataSource = (String)entry.get(DominoNSFConfiguration.COL_INDEX_DATASOURCE, String.class, null);
							String envJson = (String)entry.get(DominoNSFConfiguration.COL_INDEX_ENV, String.class, null);
							
							FileSystemMountProvider provider = providers.stream()
								.filter(p -> p.getName().equals(type))
								.findFirst()
								.orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("Unable to find FileSystemMountProvider for type \"{0}\"", type)));
							
							if(log.isLoggable(Level.FINEST)) {
								log.finest(MessageFormat.format("Found provider: {0}", provider));
							}
							
							Map<String, Object> env = new HashMap<>();
							
							if(envJson != null && !envJson.trim().isEmpty()) {
								try {
									@SuppressWarnings("unchecked")
									Map<String, Object> parsedEnv = (Map<String, Object>)JsonParser.fromJson(JsonJavaFactory.instance, envJson);
									env.putAll(parsedEnv);
								} catch(JsonException e) {
									throw new RuntimeException(MessageFormat.format("Could not parse JSON: {0}", envJson));
								}
							}
							
							env.put("username", username); //$NON-NLS-1$
							
							try {
								FileSystem fs = provider.createFileSystem(dataSource, env);
								result.put(path, fs);
							} catch(IOException e) {
								throw new UncheckedIOException(MessageFormat.format("Encountered exception building filesystem for mount \"{0}\"", path), e);
							}
						}
						return Action.Continue;
					}
					
					@Override
					public Map<String, FileSystem> end(Map<String, FileSystem> result) {
						return result;
					}
					
				});
			} catch(Throwable t) {
				if(log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Encountered exception while building composite filesystem");
				}
				throw t;
			}
		});
		
		if(log.isLoggable(Level.INFO)) {
			log.info(MessageFormat.format("Built filesystem list: {0}", fileSystems));
		}
		
		return new CompositeFileSystem(CompositeFileSystemProvider.instance, fileSystems);
	}
	
	private Optional<Document> getServerDoc(DominoClient client) {
		try {
		Database configNsf = client.openDatabase(getConfigNsfPath());
		DominoCollection serverDocs = configNsf.openCollection(VIEW_CONFIG)
			.orElseThrow(() -> new IllegalStateException(MessageFormat.format("Unable to open view \"{0}\" in database \"{1}\"", VIEW_CONFIG, getConfigNsfPath())));
		return serverDocs.query()
			.selectByKey(client.getIDUserName(), true)
			.firstId()
			.flatMap(configNsf::getDocumentById);
		} catch(Throwable t) {
			t.printStackTrace();
			return Optional.empty();
		}
	}
}
