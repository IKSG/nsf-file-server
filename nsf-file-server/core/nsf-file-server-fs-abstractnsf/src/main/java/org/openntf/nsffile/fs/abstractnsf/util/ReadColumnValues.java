package org.openntf.nsffile.fs.abstractnsf.util;

import java.util.ArrayList;
import java.util.List;

import com.hcl.domino.data.CollectionEntry;
import com.hcl.domino.data.CollectionSearchQuery.CollectionEntryProcessor;
import com.hcl.domino.data.Database.Action;

public class ReadColumnValues<T> implements CollectionEntryProcessor<List<T>> {
	
	private final int column;
	private final Class<T> clazz;
	private final T defaultValue;
	
	public ReadColumnValues(int column, Class<T> clazz, T defaultValue) {
		this.column = column;
		this.clazz = clazz;
		this.defaultValue = defaultValue;
	}

	@Override
	public List<T> start() {
		return new ArrayList<>();
	}

	@Override
	public List<T> end(List<T> result) {
		return result;
	}

	@Override
	public Action entryRead(List<T> result, CollectionEntry entry) {
		result.add(entry.get(column, clazz, defaultValue));
		return Action.Continue;
	}
	
}
