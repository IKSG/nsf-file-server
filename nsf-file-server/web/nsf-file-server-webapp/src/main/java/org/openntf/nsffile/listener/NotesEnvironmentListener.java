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
package org.openntf.nsffile.listener;

import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.openntf.nsffile.util.NotesThreadFactory;

import com.ibm.domino.napi.c.C;

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
		String path = NotesThreadFactory.call(session -> session.getEnvironmentString("NotesProgram", true)); //$NON-NLS-1$
		System.setProperty("java.library.path", path); //$NON-NLS-1$
		C.initLibrary(null);
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
