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
package org.openntf.nsffile.fs.abstractnsf.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.Set;

import org.openntf.nsffile.core.fs.attribute.NSFFileAttributes;
import org.openntf.nsffile.fs.abstractnsf.NSFPath;
import org.openntf.nsffile.fs.abstractnsf.attribute.RootFileAttributes;

/**
 * API for implementing the specifics of reading from an NSF.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public interface NSFAccessor {
	/**
	 * Returns a list of file names for files within the provided directory.
	 * 
	 * @param dir the directory to list
	 * @return a {@link List} of individual file names, in alphabetical order
	 */
	List<String> getDirectoryEntries(NSFPath dir);
	
	/**
	 * Extracts the attachment from the provided NSF path.
	 * 
	 * <p>The extracted file will have the same name as the {code path}'s file name, and
	 * will be housed within a temporary directory. Both the returned file and its parent
	 * should be deleted when no longer needed.</p>
	 * 
	 * @param path the path of the file to extract
	 * @return a {@link Path} to a temporary file holding the attachment contents
	 */
	Path extractAttachment(NSFPath path);
	
	/**
	 * Stores the provided attachment data in the named path.
	 * 
	 * @param path the path to the file inside the NSF
	 * @param attachmentData the path to the attachment data stored on disk
	 * @throws IOException if there is a problem attaching the data
	 */
	void storeAttachment(NSFPath path, Path attachmentData) throws IOException;
	
	/**
	 * Creates a directory entry for the provided path, if it doesn't currently exist.
	 * 
	 * @param dir the path of the desired directory
	 * @param attrs the attributes of the directory to create
	 * @throws IOException if there is a problem creating the directory document
	 */
	void createDirectory(NSFPath dir, FileAttribute<?>... attrs) throws IOException;

	/**
	 * Deletes the file or folder at the provided path, if it exists.
	 * 
	 * @param path the path of the file to delete
	 * @throws IOException if there is a problem deleting the file
	 */
	void delete(NSFPath path) throws IOException;
	
	/**
	 * Copies the provided source note to the target, deleting the target if it exists.
	 * 
	 * @param source the source path to copy
	 * @param target the target path
	 * @param options Java NIO copy options
	 * @throws IOException if there is a database problem copying the file
	 */
	void copy(NSFPath source, NSFPath target, CopyOption... options) throws IOException;

	/**
	 * Moves the provided source note to the target, deleting the target if it exists.
	 * 
	 * @param source the source path to move
	 * @param target the target path
	 * @param options Java NIO copy options
	 * @throws IOException if there is a database problem moving the file
	 */
	void move(NSFPath source, NSFPath target, CopyOption... options) throws IOException;
	
	/**
	 * Checks if the provided path exists in the database.
	 * 
	 * @param path the path of the file or folder to check
	 * @return whether the file currently exists in the database
	 */
	boolean exists(NSFPath path);
	
	NSFFileAttributes readAttributes(NSFPath path);
	
	/**
	 * Sets the owner of the provided path to the provided name.
	 * 
	 * @param path the path of the file or folder to set
	 * @param owner the new owner name
	 * @throws IOException if there is a database problem setting the owner
	 */
	void setOwner(NSFPath path, UserPrincipal owner) throws IOException;
	
	/**
	 * Sets the group of the provided path to the provided name.
	 * 
	 * @param path the path of the file or folder to set
	 * @param group the new group name
	 * @throws IOException if there is a database problem setting the group
	 */
	void setGroup(NSFPath path, UserPrincipal group) throws IOException;
	
	/**
	 * Sets the permissions of the provided path to the provided name.
	 * 
	 * @param path the path of the file or folder to set
	 * @param perms the new permissions
	 * @throws IOException if there is a database problem setting the permissions
	 */
	void setPermissions(NSFPath path, Set<PosixFilePermission> perms) throws IOException;
	
	/**
	 * Sets the modified and/or creation time of the provided path.
	 * 
	 * @param path the path of the file to set
	 * @param lastModifiedTime the last modified time, if desired
	 * @param createTime the creation time, if desired
	 * @throws IOException if there is a database problem setting the metadata
	 */
	void setTimes(NSFPath path, FileTime lastModifiedTime, FileTime createTime) throws IOException;
	
	/**
	 * Lists the names of any user-defined attributes on the provided path
	 * 
	 * @param path the path to check
	 * @return a {@link List} of attribute names
	 * @throws IOException if there is a DB problem reading the names
	 */
	List<String> listUserDefinedAttributes(NSFPath path) throws IOException;
	
	/**
	 * Writes the provided data to the named user-defined attribute in the path
	 * 
	 * @param path the path of the file to amend
	 * @param name the name of the user-defined attribute
	 * @param src a buffer containing the data
	 * @return the number of bytes written
	 * @throws IOException if there is a DB problem writing the data
	 */
	int writeUserDefinedAttribute(NSFPath path, String name, ByteBuffer src) throws IOException;

	/**
	 * Deletes the named user-defined attribute from the provided path
	 * 
	 * @param path the path of the file to adjust
	 * @param name the name of the user-defined attribute
	 * @throws IOException if there is a DB problem deleting the data
	 */
	void deleteUserDefinedAttribute(NSFPath path, String name) throws IOException;
	
	/**
	 * Reads the provided user-defined attribute from the provided path.
	 * 
	 * @param path the path of the file to read
	 * @param name the name of the attribute to read
	 * @return the attribute data as a byte array
	 * @throws IOException if there is a DB problem reading the data
	 */
	byte[] getUserDefinedAttribute(NSFPath path, String name) throws IOException;
	
	/**
	 * Retrieves a {@link RootFileAttributes} object for the active filesystem
	 * 
	 * @param path the path of the root
	 * @return a {@link RootFileAttributes} object for the active filesystem
	 */
	RootFileAttributes getRootFileAttributes(Path path);
}
