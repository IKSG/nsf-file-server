package org.openntf.nsffile;

import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import lombok.SneakyThrows;
import lotus.notes.NotesThread;

/**
 * Handles the Notes/Domino API lifecycle.
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NotesEnvironmentListener implements ServletContextListener {
	@Override
	@SneakyThrows
	public void contextInitialized(ServletContextEvent sce) {
		NotesThread.sinitThread();
	}
	
	@Override
	@SneakyThrows
	public void contextDestroyed(ServletContextEvent sce) {
		NotesThreadFactory.executor.shutdown();
		try {
			NotesThreadFactory.executor.awaitTermination(1, TimeUnit.MINUTES);
		} catch(InterruptedException e) {
		}
		NotesThreadFactory.scheduler.shutdown();
		try {
			NotesThreadFactory.scheduler.awaitTermination(1, TimeUnit.MINUTES);
		} catch(InterruptedException e) {
		}
		NotesThread.stermThread();
	}
}
