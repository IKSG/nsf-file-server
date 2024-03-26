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
package org.openntf.nsffile.domino.config;

import java.nio.file.FileSystem;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.commons.util.StringUtil;
import com.ibm.domino.napi.NException;
import com.ibm.domino.napi.c.Os;

import org.openntf.nsffile.commons.fs.CompositeFileSystem;
import org.openntf.nsffile.commons.fs.CompositeFileSystemProvider;
import org.openntf.nsffile.commons.util.NSFFileUtil;
import org.openntf.nsffile.commons.util.NotesThreadFactory;
import org.openntf.nsffile.ssh.spi.FileSystemMountProvider;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewNavigator;
import lotus.notes.addins.DominoServer;

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
	public static final String VIEW_CONFIG = "ServerConfigurations"; //$NON-NLS-1$
	public static final String ITEM_ENABLED = "Enabled"; //$NON-NLS-1$
	public static final String ITEM_PORT = "Port"; //$NON-NLS-1$

	private final String nsfPath;
	
	private DominoNSFConfiguration() {
		try {
			String envProperty = Os.OSGetEnvironmentString(ENV_DBPATH);
			if(StringUtil.isEmpty(envProperty)) {
				envProperty = DEFAULT_DBPATH;
			}
			this.nsfPath = envProperty;
		} catch(NException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getConfigNsfPath() {
		return nsfPath;
	}
	
	public boolean isEnabled() {
		return NotesThreadFactory.call(dominoSession -> {
			Database configNsf = NSFFileUtil.openDatabase(dominoSession, DominoNSFConfiguration.instance.getConfigNsfPath());
			View mounts = configNsf.getView(DominoNSFConfiguration.VIEW_CONFIG);
			Document serverDoc = mounts.getDocumentByKey(dominoSession.getUserName(), true);
			if(serverDoc == null) {
				return false;
			} else {
				return !"N".equals(serverDoc.getItemValueString(ITEM_ENABLED)); //$NON-NLS-1$
			}
		});
	}
	
	public int getPort() {
		return NotesThreadFactory.call(dominoSession -> {
			Database configNsf = NSFFileUtil.openDatabase(dominoSession, DominoNSFConfiguration.instance.getConfigNsfPath());
			View mounts = configNsf.getView(DominoNSFConfiguration.VIEW_CONFIG);
			Document serverDoc = mounts.getDocumentByKey(dominoSession.getUserName(), true);
			if(serverDoc == null) {
				return DEFAULT_PORT;
			} else {
				int port = serverDoc.getItemValueInteger(ITEM_PORT);
				return port == 0 ? DEFAULT_PORT : port;
			}
		});
	}
	
	public CompositeFileSystem buildFileSystem(String username) {
		// Read the view to create filesystems for each entry
		Map<String, FileSystem> fileSystems = NotesThreadFactory.call(dominoSession -> {
			try {
				DominoServer server = new DominoServer();
				@SuppressWarnings("unchecked")
				Collection<String> names = server.getNamesList(dominoSession.getUserName());
				
				List<FileSystemMountProvider> providers = NSFFileUtil.findExtensions(FileSystemMountProvider.class);
				if(log.isLoggable(Level.FINEST)) {
					log.finest(MessageFormat.format("Found providers: {0}", providers));
				}
				
				Map<String, FileSystem> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				
				Database configNsf = NSFFileUtil.openDatabase(dominoSession, DominoNSFConfiguration.instance.getConfigNsfPath());
				View mounts = configNsf.getView(DominoNSFConfiguration.VIEW_MOUNTS);
				ViewNavigator nav = mounts.createViewNav();
				ViewEntry entry = nav.getFirst();
				while(entry != null) {
					entry.setPreferJavaDates(true);
					List<?> columnValues = entry.getColumnValues();
					
					if(log.isLoggable(Level.FINEST)) {
						log.finest(MessageFormat.format("Processing entry {0}", columnValues));
					}
					
					List<String> servers = NSFFileUtil.toStringList(columnValues.get(DominoNSFConfiguration.COL_INDEX_SERVERS));
					if(servers.stream().anyMatch(s -> names.contains(s))) {
						if(log.isLoggable(Level.FINEST)) {
							log.finest("Acceptable for current server");
						}
						
						String path = (String)columnValues.get(DominoNSFConfiguration.COL_INDEX_PATH);
						String type = (String)columnValues.get(DominoNSFConfiguration.COL_INDEX_TYPE);
						String dataSource = (String)columnValues.get(DominoNSFConfiguration.COL_INDEX_DATASOURCE);
						
						FileSystemMountProvider provider = providers.stream()
							.filter(p -> p.getName().equals(type))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("Unable to find FileSystemMountProvider for type \"{0}\"", type)));
						
						if(log.isLoggable(Level.FINEST)) {
							log.finest(MessageFormat.format("Found provider: {0}", provider));
						}
						
						Map<String, Object> env = new HashMap<>();
						env.put("username", username); //$NON-NLS-1$
						FileSystem fs = provider.createFileSystem(dataSource, env);
						result.put(path, fs);
					}
					
					ViewEntry tempEntry = entry;
					entry = nav.getNext();
					tempEntry.recycle();
				}
				return result;
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
}
