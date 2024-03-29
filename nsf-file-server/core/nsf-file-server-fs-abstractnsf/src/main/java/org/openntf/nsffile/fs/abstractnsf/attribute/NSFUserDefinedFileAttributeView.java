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
package org.openntf.nsffile.fs.abstractnsf.attribute;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.LinkOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;

import org.openntf.nsffile.fs.abstractnsf.NSFPath;
import org.openntf.nsffile.fs.abstractnsf.db.NSFAccessor;

/**
 * Implementation of {@link UserDefinedFileAttributeView} that stores user-defined attributes
 * in custom data fields in items prefixed with
 * {@value org.openntf.nsffile.fs.abstractnsf.NSFFileSystemConstants#PREFIX_USERITEM}.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NSFUserDefinedFileAttributeView implements UserDefinedFileAttributeView {
	private final NSFAccessor accessor;
	private final NSFPath path;
	
	public NSFUserDefinedFileAttributeView(NSFAccessor accessor, NSFPath path, LinkOption... options) {
		this.accessor = accessor;
		this.path = path;
    }

	@Override
	public String name() {
		return "user"; //$NON-NLS-1$
	}

	@Override
	public List<String> list() throws IOException {
		return accessor.listUserDefinedAttributes(this.path);
	}

	@Override
	public int size(String name) throws IOException {
		return accessor.getUserDefinedAttribute(this.path, name).length;
	}

	@Override
	public int read(String name, ByteBuffer dst) throws IOException {
		byte[] value = accessor.getUserDefinedAttribute(this.path, name);
		dst.put(value);
		return value.length;
	}

	@Override
	public int write(String name, ByteBuffer src) throws IOException {
		return accessor.writeUserDefinedAttribute(this.path, name, src);
	}

	@Override
	public void delete(String name) throws IOException {
		accessor.deleteUserDefinedAttribute(this.path, name);
	}
}
