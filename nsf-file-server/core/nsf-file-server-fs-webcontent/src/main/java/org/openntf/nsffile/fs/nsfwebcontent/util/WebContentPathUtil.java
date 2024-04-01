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
package org.openntf.nsffile.fs.nsfwebcontent.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openntf.nsffile.core.util.NSFFileUtil;
import org.openntf.nsffile.core.util.NotesThreadFactory;
import org.openntf.nsffile.core.util.TimedCacheHolder;
import org.openntf.nsffile.fs.abstractnsf.NSFFileSystem;
import org.openntf.nsffile.fs.abstractnsf.NSFPath;
import org.openntf.nsffile.fs.abstractnsf.function.NotesDatabaseConsumer;
import org.openntf.nsffile.fs.abstractnsf.function.NotesDatabaseFunction;
import org.openntf.nsffile.fs.abstractnsf.function.NotesDocumentConsumer;
import org.openntf.nsffile.fs.abstractnsf.function.NotesDocumentFunction;
import org.openntf.nsffile.fs.nsfwebcontent.WebContentFileSystemProvider;
import org.openntf.nsffile.fs.nsfwebcontent.db.WebContentNSFAccessor;

import com.hcl.domino.DominoClient;
import com.hcl.domino.data.Database;
import com.hcl.domino.data.Document;
import com.hcl.domino.data.DominoDateTime;
import com.hcl.domino.misc.Ref;
import com.ibm.commons.util.StringUtil;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
@SuppressWarnings("nls")
public enum WebContentPathUtil {
	;
	
	private static final Logger log = Logger.getLogger(WebContentPathUtil.class.getPackage().getName());
	
	public static final String LOCAL_SERVER = "LOCALSERVER"; //$NON-NLS-1$
	
	private static final Function<String, String> encoder = path -> StringUtil.isEmpty(path) ? "" : //$NON-NLS-1$
		Base64.getUrlEncoder().encodeToString(path.getBytes()).replace('=', '-') + "END"; //$NON-NLS-1$
	private static final Function<String, String> decoder = enc -> StringUtil.isEmpty(enc) ? "" : //$NON-NLS-1$
		new String(Base64.getUrlDecoder().decode(enc.replace('-', '=').substring(0, enc.length()-"END".length()).getBytes()));; //$NON-NLS-1$
	
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
		Objects.requireNonNull(uri, "uri cannot be null"); //$NON-NLS-1$
		
		String host = decoder.apply(uri.getHost());
		if(LOCAL_SERVER.equals(host)) {
			host = null;
		}
		String pathInfo = uri.getPath().substring(1);
		if(pathInfo == null || pathInfo.isEmpty()) {
			throw new IllegalArgumentException("URI path info cannot be empty");
		}
		
		String nsfPath;
		int nsfIndex = pathInfo.indexOf('/');
		if(nsfIndex < 0) {
			nsfPath = decoder.apply(pathInfo);
		} else {
			nsfPath = decoder.apply(pathInfo.substring(0, nsfIndex));
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
		if(pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) { //$NON-NLS-1$
			throw new IllegalArgumentException("URI path info cannot be empty");
		}
		pathInfo = pathInfo.substring(1); // Chop off the initial /
		
		int nsfIndex = pathInfo.indexOf('/');
		if(nsfIndex < 0) {
			return ""; //$NON-NLS-1$
		} else {
			return pathInfo.substring(nsfIndex);
		}
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
			nsfPath = apiPath;
		} else {
			host = apiPath.substring(0, bangIndex);
			nsfPath = apiPath.substring(bangIndex+2);
		}
		host = encoder.apply(host);
		nsfPath = "/" + encoder.apply(nsfPath); //$NON-NLS-1$
		return new URI(WebContentFileSystemProvider.SCHEME, userName, host, -1, nsfPath, null, null);
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
		
		URI base = toFileSystemURI(userName, apiPath);
		
		String pathInfo = NSFFileUtil.concat("/", base.getPath()); //$NON-NLS-1$
		if(StringUtil.isNotEmpty(pathBit)) {
			pathInfo = NSFFileUtil.concat(pathInfo, pathBit);
		}
		for(String bit : morePathBits) {
			if(StringUtil.isNotEmpty(bit)) {
				pathInfo = NSFFileUtil.concat(pathInfo, bit);
			}
		}
		
		return new URI(WebContentFileSystemProvider.SCHEME, userName, base.getHost(), -1, pathInfo, null, null);
	}
	
	/**
	 * Executes the provided function with a document for the provided path.
	 * 
	 * @param <T> the type returned by {@code func}
	 * @param path the context {@link NSFPath}
	 * @param cacheId an identifier used to cache the result based on the database modification
	 * 			time. Pass {@code null} to skip cache
	 * @param func the function to call
	 * @return the return value of {@code func}
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static <T> T callWithDocument(NSFPath path, String cacheId, NotesDocumentFunction<T> func) {
		return callWithDatabase(path, cacheId, database -> {
			Document doc = WebContentNSFAccessor.getDocument(path, database);
			return func.apply(doc);
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
			Document doc = WebContentNSFAccessor.getDocument(path, database);
			consumer.accept(doc);
		});
	}
	
	private static final Map<String, TimedCacheHolder> PER_DATABASE_CACHE = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Executes the provided function with the database for the provided path.
	 * 
	 * @param <T> the type returned by {@code func}
	 * @param path the context {@link NSFPath}
	 * @param cacheId an identifier used to cache the result based on the database modification
	 * 			time. Pass {@code null} to skip cache
	 * @param func the function to call
	 * @return the return value of {@code func}
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	@SuppressWarnings("unchecked")
	public static <T> T callWithDatabase(NSFPath path, String cacheId, NotesDatabaseFunction<T> func) {
		return NotesThreadFactory.callAs(NSFFileUtil.dn(path.getFileSystem().getUserName()), client -> {
			Database database = getDatabase(client, path.getFileSystem());
			if(StringUtil.isEmpty(cacheId)) {
				return func.apply(database);
			} else {
				Ref<DominoDateTime> mod = new Ref<>();
				database.getModifiedTime(null, mod);
				long modTime = Instant.from(mod.get()).toEpochMilli();
				String dbKey = database.getRelativeFilePath() + "//" + client.getEffectiveUserName(); //$NON-NLS-1$
				TimedCacheHolder cacheHolder = PER_DATABASE_CACHE.computeIfAbsent(dbKey, key -> new TimedCacheHolder());
				return (T)cacheHolder.get(modTime).computeIfAbsent(cacheId, key -> {
					try {
						return func.apply(database);
					} catch (Exception e) {
						if(log.isLoggable(Level.SEVERE)) {
							log.log(Level.SEVERE, MessageFormat.format("Encountered exception accessing database for path {0}", path), e);
						}
						throw new RuntimeException(e);
					}
				});
			}
		});
	}
	
	/**
	 * Invalidates any in-memory cache for the provided database.
	 * 
	 * @param database 
	 */
	public static synchronized void invalidateDatabaseCache(Database database) {
		String dbKeyPrefix = database.getRelativeFilePath();
		Iterator<String> iter = PER_DATABASE_CACHE.keySet().iterator();
		while(iter.hasNext()) {
			String key = iter.next();
			if(key.startsWith(dbKeyPrefix+"//")) { //$NON-NLS-1$
				iter.remove();
			}
		}
	}

	/**
	 * Executes the provided function with the database for the provided path.
	 * 
	 * @param path the context {@link NSFPath}
	 * @param consumer the function to call
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static void runWithDatabase(NSFPath path, NotesDatabaseConsumer consumer) {
		NotesThreadFactory.runAs(NSFFileUtil.dn(path.getFileSystem().getUserName()), session -> {
			Database database = getDatabase(session, path.getFileSystem());
			consumer.accept(database);
		});
	}
	
	
	// *******************************************************************************
	// * Internal utilities
	// *******************************************************************************
	
	private static Database getDatabase(DominoClient client, NSFFileSystem fileSystem) {
		String nsfPath = fileSystem.getNsfPath();
		return client.openDatabase(nsfPath);
	}

	
	public static String toFileName(NSFPath path) {
		String p = path.toAbsolutePath().toString();
		if(p.startsWith("/")) { //$NON-NLS-1$
			p = p.substring(1);
		}
		return p;
	}
}
