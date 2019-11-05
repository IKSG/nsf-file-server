package org.openntf.nsffile.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openntf.nsffile.util.NotesThreadFactory;

import lotus.domino.Database;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewNavigator;

public class NSFDirectoryStream implements DirectoryStream<Path> {
	
	private final List<Path> paths;

	@SuppressWarnings("unchecked")
	public NSFDirectoryStream(NSFFileSystemProvider provider, NSFPath dir) {
		try {
			this.paths = NotesThreadFactory.executor.submit(() -> {
				try {
					Database database = provider.getDatabase(dir.getFileSystem());
					View filesByParent = database.getView("Files By Parent");
					filesByParent.setAutoUpdate(false);
					
					String category = dir.toAbsolutePath().toString();
					ViewNavigator nav = filesByParent.createViewNavFromCategory(category);
					nav.setBufferMaxEntries(400);
					List<Path> result = new ArrayList<>(nav.getCount());
					ViewEntry entry = nav.getFirst();
					while(entry != null) {
						entry.setPreferJavaDates(true);
						String name = String.valueOf(entry.getColumnValues().get(2));
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
			}).get();
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
