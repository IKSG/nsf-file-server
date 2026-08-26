/**
 * Copyright (c) 2019-2026 Jesse Gallagher
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
package org.openntf.nsffile.fs.filesilo.util;

import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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
import org.openntf.nsffile.fs.filesilo.db.FileSiloNSFAccessor;

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
public enum FileSiloPathUtil {
	;
	
	private static final Logger log = Logger.getLogger(FileSiloPathUtil.class.getPackage().getName());
	
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
			Document doc = FileSiloNSFAccessor.getDocument(path, database);
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
			Document doc = FileSiloNSFAccessor.getDocument(path, database);
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
				database.getModifiedTime(mod, null);
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
	
	/**
	 * Generates a random-enough alphanumeric string key for use with new files.
	 * 
	 * @return a random-enough string
	 */
	public static String generateRandomKey() {
		StringBuilder result = new StringBuilder();
		result.append(Long.toHexString(System.nanoTime()));
		result.append(Long.toHexString(System.nanoTime()));
		return result.toString();
	}
	
	// *******************************************************************************
	// * Internal utilities
	// *******************************************************************************
	
	private static Database getDatabase(DominoClient client, NSFFileSystem fileSystem) {
		String nsfPath = fileSystem.getNsfPath();
		return client.openDatabase(nsfPath);
	}
}
