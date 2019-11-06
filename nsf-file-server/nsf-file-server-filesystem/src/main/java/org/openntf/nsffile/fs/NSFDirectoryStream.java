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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openntf.nsffile.fs.util.NSFPathUtil;

import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewNavigator;

import static org.openntf.nsffile.fs.NSFFileSystemConstants.*;

public class NSFDirectoryStream implements DirectoryStream<Path> {
	
	private final List<Path> paths;

	@SuppressWarnings("unchecked")
	public NSFDirectoryStream(NSFFileSystemProvider provider, NSFPath dir) {
		try {
			this.paths = NSFPathUtil.callWithDatabase(dir, database -> {
				try {
					View filesByParent = database.getView(VIEW_FILESBYPARENT);
					filesByParent.setAutoUpdate(false);
					filesByParent.refresh();
					
					String category = dir.toAbsolutePath().toString();
					ViewNavigator nav = filesByParent.createViewNavFromCategory(category);
					nav.setBufferMaxEntries(400);
					List<Path> result = new ArrayList<>(nav.getCount());
					ViewEntry entry = nav.getFirst();
					while(entry != null) {
						entry.setPreferJavaDates(true);
						String name = String.valueOf(entry.getColumnValues().get(VIEW_FILESBYPARENT_INDEX_NAME));
						result.add(dir.resolve(name));
						
						ViewEntry tempEntry = entry;
						entry = nav.getNext();
						tempEntry.recycle();
					}
					
					return result;
				} catch(Throwable t) {
					t.printStackTrace();
					return Collections.EMPTY_LIST;
				}
			});
		} catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public Iterator<Path> iterator() {
		return paths.iterator();
	}

	@Override
	public void close() throws IOException {
	}
}
