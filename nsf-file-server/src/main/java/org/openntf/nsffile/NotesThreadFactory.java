package org.openntf.nsffile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import lotus.domino.NotesThread;

public class NotesThreadFactory implements ThreadFactory {
	public static final NotesThreadFactory instance = new NotesThreadFactory();
	public static final ExecutorService executor = Executors.newCachedThreadPool(instance);
	public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5, instance);

	@Override
	public Thread newThread(Runnable r) {
		return new NotesThread(r);
	}

}
