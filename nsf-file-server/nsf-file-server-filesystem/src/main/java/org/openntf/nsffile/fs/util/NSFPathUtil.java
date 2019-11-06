/**
 * Copyright © 2019 Jesse Gallagher
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
package org.openntf.nsffile.fs.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openntf.nsffile.fs.NSFFileSystem;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.fs.NSFPath;
import org.openntf.nsffile.util.NotesThreadFactory;

import com.ibm.commons.util.PathUtil;
import com.ibm.commons.util.StringUtil;
import com.ibm.designer.domino.napi.NotesConstants;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;

import static org.openntf.nsffile.fs.NSFFileSystemConstants.*;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public enum NSFPathUtil {
	;
	
	public static final String LOCAL_SERVER = "LOCALSERVER"; //$NON-NLS-1$
	public static final String NAME_DELIM = "NAMEDELIM"; //$NON-NLS-1$
	public static final String PREFIX_REPID = "REPLICAID"; //$NON-NLS-1$
	
	/**
	 * Extracts the NSF API path from the provided URI. For example:
	 * 
	 * <ul>
	 *   <li>{@code "nsffile:///foo.nsf/bar} &rarr; {@code "foo.nsf"}</li>
	 *   <li>{@code "nsffile://someserver/foo.nsf/bar} &rarr; {@code "someserver!!foo.nsf"}
	 * </ul> 
	 * 
	 * @param uri the URI from which to extract the NSF path
	 * @return the NSF path in API format
	 * @throws IllegalArgumentException if {@code uri} is {@code null} or does not contain an NSF name
	 * @since 1.0.0
	 */
	public static String extractApiPath(URI uri) {
		Objects.requireNonNull(uri, "uri cannot be null");
		
		String host = uri.getHost();
		if(LOCAL_SERVER.equals(host)) {
			host = null;
		} else if(StringUtil.isNotEmpty(host)) {
			host = host.replace(NAME_DELIM, "/"); //$NON-NLS-1$
		}
		String pathInfo = uri.getPath();
		if(pathInfo == null || pathInfo.isEmpty()) {
			throw new IllegalArgumentException("URI path info cannot be empty");
		}
		
		String nsfPath;
		int nsfIndex = pathInfo.toLowerCase().indexOf(".nsf"); //$NON-NLS-1$
		if(nsfIndex < 2) {
			// Check if it's a replica ID
			String repId = pathInfo.substring(1);
			if(repId.startsWith(PREFIX_REPID)) {
				nsfPath = repId.substring(PREFIX_REPID.length());
			} else {
				throw new IllegalArgumentException("Unable to extract NSF path from " + uri);
			}
		} else {
			nsfPath = pathInfo.substring(1, nsfIndex+4);
		}
		if(host == null || host.isEmpty()) {
			return nsfPath;
		} else {
			return host + "!!" + nsfPath; //$NON-NLS-1$
		}
	}
	
	/**
	 * Extracts the in-NSF file path from the provided URI. For example:
	 * 
	 * <ul>
	 *   <li>{@code "nsffile:///foo.nsf/bar} &rarr; {@code "/bar"}</li>
	 *   <li>{@code "nsffile://someserver/foo.nsf/bar/baz} &rarr; {@code "/bar/baz"}
	 * </ul> 
	 * 
	 * @param uri the URI from which to extract the file path
	 * @return the relative file path
	 * @throws IllegalArgumentException if {@code uri} is {@code null} or does not contain an NSF name
	 * @since 1.0.0
	 */
	public static String extractPathInfo(URI uri) {
		Objects.requireNonNull(uri, "uri cannot be null");
		
		String pathInfo = uri.getPath();
		if(pathInfo == null || pathInfo.isEmpty()) {
			throw new IllegalArgumentException("URI path info cannot be empty");
		}
		
		int nsfIndex = pathInfo.toLowerCase().indexOf(".nsf"); //$NON-NLS-1$
		if(nsfIndex < 2) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri);
		}
		return pathInfo.substring(nsfIndex+4);
	}
	
	/**
	 * Converts a provided NSF API path to a {@link URI} object referencing the {@code "nsffilestore"}
	 * filesystem.
	 * 
	 * @param apiPath the API path to convert
	 * @return the URI version of the API path
	 * @throws URISyntaxException 
	 * @since 1.0.0
	 * @throws IllegalArgumentException if {@code apiPath} is empty
	 */
	public static URI toFileSystemURI(String userName, String apiPath) throws URISyntaxException {
		if(StringUtil.isEmpty(apiPath)) {
			throw new IllegalArgumentException("apiPath cannot be empty");
		}
		
		int bangIndex = apiPath.indexOf("!!"); //$NON-NLS-1$
		String host;
		String nsfPath;
		if(bangIndex < 0) {
			host = LOCAL_SERVER;
			nsfPath = "/" + apiPath //$NON-NLS-1$
				.replace('\\', '/')
				.replace(":", ""); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			host = apiPath
				.substring(0, bangIndex)
				.replace("/", NAME_DELIM); //$NON-NLS-1$
			nsfPath = "/" + apiPath //$NON-NLS-1$
				.substring(bangIndex+2)
				.replace('\\', '/')
				.replace(":", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if(isReplicaID(nsfPath.substring(1))) {
			nsfPath = "/" + PREFIX_REPID + nsfPath.substring(1); //$NON-NLS-1$
		}
		return new URI(NSFFileSystemProvider.SCHEME, userName, host, -1, nsfPath, null, null);
	}
	
	/**
	 * Converts a provided NSF API path to a {@link URI} object referencing the {@code "nsffilestore"}
	 * filesystem.
	 * 
	 * @param apiPath the API path to convert
	 * @return the URI version of the API path
	 * @throws URISyntaxException 
	 * @since 1.0.0
	 * @throws IllegalArgumentException if {@code apiPath} is empty
	 */
	public static URI toFileSystemURI(String userName, String apiPath, String pathBit, String... morePathBits) throws URISyntaxException {
		if(StringUtil.isEmpty(apiPath)) {
			throw new IllegalArgumentException("apiPath cannot be empty");
		}
		
		int bangIndex = apiPath.indexOf("!!"); //$NON-NLS-1$
		String host;
		String nsfPath;
		if(bangIndex < 0) {
			host = LOCAL_SERVER;
			nsfPath = "/" + apiPath //$NON-NLS-1$
				.replace('\\', '/')
				.replace(":", ""); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			host = apiPath
				.substring(0, bangIndex)
				.replace("/", NAME_DELIM); //$NON-NLS-1$
			nsfPath = "/" + apiPath //$NON-NLS-1$
				.substring(bangIndex+2)
				.replace('\\', '/')
				.replace(":", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if(isReplicaID(nsfPath.substring(1))) {
			nsfPath = "/" + PREFIX_REPID + nsfPath.substring(1); //$NON-NLS-1$
		}
		
		String pathInfo = concat("/", nsfPath); //$NON-NLS-1$
		if(StringUtil.isNotEmpty(pathBit)) {
			pathInfo = concat(pathInfo, pathBit);
		}
		for(String bit : morePathBits) {
			if(StringUtil.isNotEmpty(bit)) {
				pathInfo = concat(pathInfo, bit);
			}
		}
		
		return new URI(NSFFileSystemProvider.SCHEME, userName, host, -1, pathInfo, null, null);
	}
	
	public static String concat(final char delim, final String... parts) {
		if(parts == null || parts.length == 0) {
			return StringUtil.EMPTY_STRING;
		}
		String path = parts[0];
		for(int i = 1; i < parts.length; i++) {
			path = PathUtil.concat(path, parts[i], delim);
		}
		return path;
	}

	public static String concat(final String... parts) {
		return concat('/', parts);
	}
	
	@FunctionalInterface
	public static interface NotesDocumentFunction<T> {
		T apply(Document doc) throws Exception;
	}
	
	@FunctionalInterface
	public static interface NotesDocumentConsumer {
		void accept(Document doc) throws Exception;
	}
	
	@FunctionalInterface
	public static interface NotesDatabaseFunction<T> {
		T apply(Database doc) throws Exception;
	}
	
	@FunctionalInterface
	public static interface NotesDatabaseConsumer {
		void accept(Database doc) throws Exception;
	}
	
	/**
	 * Executes the provided function with a document for the provided path.
	 * 
	 * @param <T> the type returned by {@code func}
	 * @param path the context {@link NSFPath}
	 * @param func the function to call
	 * @return the return value of {@code func}
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static <T> T callWithDocument(NSFPath path, NotesDocumentFunction<T> func) {
		return callWithDatabase(path, database-> {
			Document doc = getDocument(path, database);
			try {
				return func.apply(doc);
			} finally {
				doc.recycle();
			}
		});
	}
	
	/**
	 * Executes the provided function with a document for the provided path.
	 * 
	 * @param path the context {@link NSFPath}
	 * @param consumer the consumer to call
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static void runWithDocument(NSFPath path, NotesDocumentConsumer consumer) {
		runWithDatabase(path, database -> {
			Document doc = getDocument(path, database);
			try {
				consumer.accept(doc);
			} finally {
				doc.recycle();
			}
		});
	}

	/**
	 * Executes the provided function with the database for the provided path.
	 * 
	 * @param <T> the type returned by {@code func}
	 * @param path the context {@link NSFPath}
	 * @param func the function to call
	 * @return the return value of {@code func}
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static <T> T callWithDatabase(NSFPath path, NotesDatabaseFunction<T> func) {
		return NotesThreadFactory.callAs(dn(path.getFileSystem().getUserName()), session -> {
			Database database = getDatabase(session, path.getFileSystem());
			return func.apply(database);
		});
	}
	
	private static final ThreadLocal<Map<String, Database>> THREAD_DATABASES = ThreadLocal.withInitial(HashMap::new);

	/**
	 * Executes the provided function with the database for the provided path.
	 * 
	 * @param <T> the type returned by {@code func}
	 * @param path the context {@link NSFPath}
	 * @param func the function to call
	 * @return the return value of {@code func}
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static void runWithDatabase(NSFPath path, NotesDatabaseConsumer consumer) {
		NotesThreadFactory.runAs(dn(path.getFileSystem().getUserName()), session -> {
			Database database = getDatabase(session, path.getFileSystem());
			consumer.accept(database);
		});
	}
	
	/**
	 * Retrieves the document for the provided path, creating a new in-memory document
	 * if needed.
	 * 
	 * @param path the path to find the document for
	 * @param database the database housing the document
	 * @return a document representing the note
	 * @throws NotesException 
	 */
	public static Document getDocument(NSFPath path, Database database) throws NotesException {
		View view = database.getView(VIEW_FILESBYPATH);
		try {
			view.setAutoUpdate(false);
			view.refresh();
			Document doc = view.getDocumentByKey(path.toAbsolutePath().toString(), true);
			if(doc == null) {
				doc = database.createDocument();
				doc.replaceItemValue(ITEM_PARENT, path.getParent().toAbsolutePath().toString());
				doc.replaceItemValue(NotesConstants.ITEM_META_TITLE, path.getFileName().toString());
			}
			return doc;
		} finally {
			if(view != null) {
				view.recycle();
			}
		}
	}

	public static String shortCn(String name) {
		return NotesThreadFactory.call(session -> {
			Name n = session.createName(name);
			try {
				return n.getCommon().replaceAll("\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
			} finally {
				n.recycle();
			}
		});
	}
	
	// *******************************************************************************
	// * Internal utilities
	// *******************************************************************************
	
	private static String dn(String name) {
		return NotesThreadFactory.call(session -> session.createName(name).getCanonical());
	}
	
	private static Database getDatabase(Session session, NSFFileSystem fileSystem) throws NotesException {
		String nsfPath = fileSystem.getNsfPath();
		String key = session.getEffectiveUserName() + nsfPath;
		return THREAD_DATABASES.get().computeIfAbsent(key, k -> {
			try {
				int bangIndex = nsfPath.indexOf("!!"); //$NON-NLS-1$
				String server;
				String dbPath;
				if(bangIndex < 0) {
					server = ""; //$NON-NLS-1$
					dbPath = nsfPath;
				} else {
					server = nsfPath.substring(0, bangIndex);
					dbPath = nsfPath.substring(bangIndex+2);
				}
				if(isReplicaID(dbPath)) {
					Database database = session.getDatabase(null, null);
					database.openByReplicaID(server, dbPath);
					return database;
				} else {
					return session.getDatabase(server, dbPath);
				}
			} catch(NotesException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	private static boolean isReplicaID(String dbPath) {
		String id = normalizeReplicaID(dbPath);
		if (id == null) {
			return false;
		} else {
			for (int i = 0; i < 16; ++i) {
				if ("0123456789ABCDEF".indexOf(id.charAt(i)) < 0) { //$NON-NLS-1$
					return false;
				}
			}

			return true;
		}
	}
	
	private static String normalizeReplicaID(String id) {
		String replicaId = id;
		if (StringUtil.isNotEmpty(replicaId)) {
			if (replicaId.indexOf(':') == 8 && replicaId.length() == 17) {
				replicaId = replicaId.substring(0, 8) + replicaId.substring(9, 17);
			}

			if (replicaId.length() == 16) {
				return replicaId.toUpperCase();
			}
		}

		return null;
	}
}
