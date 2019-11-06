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
package org.openntf.nsffile.fs.attribute;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.LinkOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;
import java.util.stream.Collectors;

import org.openntf.nsffile.fs.NSFPath;
import org.openntf.nsffile.fs.util.NSFPathUtil;

import lotus.domino.Item;
import lotus.domino.NotesException;

/**
 * Implementation of {@link UserDefinedFileAttributeView} that stores user-defined attributes
 * in custom data fields in items prefixed with {@value #PREFIX_USERITEM}.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NSFUserDefinedFileAttributeView implements UserDefinedFileAttributeView {
	
	/** The prefix used for user-defined items created this way */
	public static final String PREFIX_USERITEM = "user."; //$NON-NLS-1$
	/** The name of the custom data type used to store custom attributes */
	public static final String DATATYPE_NAME = NSFUserDefinedFileAttributeView.class.getSimpleName();
	
	private final NSFPath path;
	
	public NSFUserDefinedFileAttributeView(NSFPath path, LinkOption... options) {
		this.path = path;
    }

	@Override
	public String name() {
		return "user"; //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> list() throws IOException {
		return NSFPathUtil.callWithDocument(this.path, doc ->
			((List<Item>)doc.getItems()).stream()
				.map(item -> {
					try {
						return item.getName();
					} catch (NotesException e) {
						throw new RuntimeException(e);
					}
				})
				.filter(name -> name.startsWith(PREFIX_USERITEM) && name.length() > PREFIX_USERITEM.length())
				.map(name -> name.substring(PREFIX_USERITEM.length()))
				.collect(Collectors.toList())
		);
	}

	@Override
	public int size(String name) throws IOException {
		return get(name).length;
	}

	@Override
	public int read(String name, ByteBuffer dst) throws IOException {
		byte[] value = get(name);
		dst.put(value);
		return value.length;
	}

	@Override
	public int write(String name, ByteBuffer src) throws IOException {
		return NSFPathUtil.callWithDocument(this.path, doc -> {
			String itemName = PREFIX_USERITEM + name;
			byte[] data = src.array();
			doc.replaceItemValueCustomDataBytes(itemName, DATATYPE_NAME, data);
			return data.length;
		});
	}

	@Override
	public void delete(String name) throws IOException {
		NSFPathUtil.runWithDocument(this.path, doc -> {
			String itemName = PREFIX_USERITEM + name;
			if(doc.hasItem(itemName)) {
				doc.removeItem(itemName);
			}
		});
	}

	private byte[] get(String name) {
		return NSFPathUtil.callWithDocument(this.path, doc -> {
			String itemName = PREFIX_USERITEM + name;
			Item item = doc.getFirstItem(itemName);
			return item == null ? new byte[0] : item.getValueCustomDataBytes(DATATYPE_NAME);
		});
	}
}
