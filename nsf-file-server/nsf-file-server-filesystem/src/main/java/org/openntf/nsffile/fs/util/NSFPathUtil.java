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
import java.util.Objects;

import org.openntf.nsffile.fs.NSFFileSystem;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.fs.NSFPath;
import org.openntf.nsffile.util.NotesThreadFactory;

import com.ibm.commons.util.PathUtil;
import com.ibm.commons.util.StringUtil;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public enum NSFPathUtil {
	;
	
	public static final String LOCAL_SERVER = "LOCALSERVER";
	public static final String NAME_DELIM = "NAMEDELIM";
	
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
			host = host.replace(NAME_DELIM, "/");
		}
		String pathInfo = uri.getPath();
		if(pathInfo == null || pathInfo.isEmpty()) {
			throw new IllegalArgumentException("URI path info cannot be empty");
		}
		
		int nsfIndex = pathInfo.toLowerCase().indexOf(".nsf");
		if(nsfIndex < 2) {
			throw new IllegalArgumentException("Unable to extract NSF path from " + uri);
		}
		String nsfPath = pathInfo.substring(1, nsfIndex+4);
		if(host == null || host.isEmpty()) {
			return nsfPath;
		} else {
			return host + "!!" + nsfPath;
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
		
		int nsfIndex = pathInfo.toLowerCase().indexOf(".nsf");
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
		
		int bangIndex = apiPath.indexOf("!!");
		if(bangIndex < 0) {
			return new URI(NSFFileSystemProvider.SCHEME, userName, LOCAL_SERVER, -1, "/" + apiPath.replace('\\', '/'), null, null);
		} else {
			String server = apiPath.substring(0, bangIndex);
			String filePath = apiPath.substring(bangIndex+2);
			return new URI(NSFFileSystemProvider.SCHEME, userName, server.replace("/", NAME_DELIM), -1, "/" + filePath.replace('\\', '/'), null, null);
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
	public static URI toFileSystemURI(String userName, String apiPath, String pathBit, String... morePathBits) throws URISyntaxException {
		if(StringUtil.isEmpty(apiPath)) {
			throw new IllegalArgumentException("apiPath cannot be empty");
		}
		
		int bangIndex = apiPath.indexOf("!!");
		String host;
		String nsfPath;
		if(bangIndex < 0) {
			host = LOCAL_SERVER;
			nsfPath = apiPath.replace('\\', '/');
			
		} else {
			host = apiPath.substring(0, bangIndex).replace("/", NAME_DELIM);
			nsfPath = apiPath.substring(bangIndex+2);
		}
		
		String pathInfo = concat("/", nsfPath);
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
			View view = database.getView("Files by Path");
			view.setAutoUpdate(false);
			view.refresh();
			Document doc = view.getDocumentByKey(path.toAbsolutePath().toString(), true);
			if(doc == null) {
				doc = database.createDocument();
				doc.replaceItemValue("Parent", path.getParent().toAbsolutePath().toString());
				doc.replaceItemValue("$$Title", path.getFileName().toString());
			}
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
		runWithDatabase(path, database-> {
			View view = database.getView("Files by Path");
			view.setAutoUpdate(false);
			view.refresh();
			Document doc = view.getDocumentByKey(path.toAbsolutePath().toString(), true);
			if(doc == null) {
				doc = database.createDocument();
				doc.replaceItemValue("Parent", path.getParent().toAbsolutePath().toString());
				doc.replaceItemValue("$$Title", path.getFileName().toString());
			}
			consumer.accept(doc);
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
		View view = database.getView("Files by Path");
		view.setAutoUpdate(false);
		view.refresh();
		Document doc = view.getDocumentByKey(path.toAbsolutePath().toString(), true);
		if(doc == null) {
			doc = database.createDocument();
			doc.replaceItemValue("Parent", path.getParent().toAbsolutePath().toString());
			doc.replaceItemValue("$$Title", path.getFileName().toString());
		}
		return doc;
	}
	
	// *******************************************************************************
	// * Internal utilities
	// *******************************************************************************
	
	private static String dn(String name) {
		return NotesThreadFactory.call(session -> session.createName(name).getCanonical());
	}
	
	private static Database getDatabase(Session session, NSFFileSystem fileSystem) throws NotesException {
		String nsfPath = fileSystem.getNsfPath();
		
		int bangIndex = nsfPath.indexOf("!!");
		if(bangIndex < 0) {
			return session.getDatabase("", nsfPath);
		} else {
			return session.getDatabase(nsfPath.substring(0, bangIndex), nsfPath.substring(bangIndex+2));
		}
	}

	public static String shortCn(String name) {
		return NotesThreadFactory.call(session -> session.createName(name).getCommon().replaceAll("\\s+", ""));
	}
}