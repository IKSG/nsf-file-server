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
package org.openntf.nsffile.fs;

/**
 * Common constants used in the filesystem implementation
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public enum NSFFileSystemConstants {
	;
	
	
	
	/** The form name used for file documents */
	public static final String FORM_FILE = "File"; //$NON-NLS-1$
	/** The form name used for folder documents */
	public static final String FORM_FOLDER = "Folder"; //$NON-NLS-1$
	/** The rich text item in File documents used to store the file attachment */
	public static final String ITEM_FILE = "File"; //$NON-NLS-1$
	/** The item used to store the string path of the parent folder */
	public static final String ITEM_PARENT = "Parent"; //$NON-NLS-1$
	
	/** The name of the view containing all files and folders by their full path */
	public static final String VIEW_FILESBYPATH = "Files by Path"; //$NON-NLS-1$
	/** The name of the view containing all files and folders by their parent path */
	public static final String VIEW_FILESBYPARENT = "Files by Parent"; //$NON-NLS-1$
	/** The index in the column values in {@link #VIEW_FILESBYPARENT} holding the file name */
	public static final int VIEW_FILESBYPARENT_INDEX_NAME = 2;
}
