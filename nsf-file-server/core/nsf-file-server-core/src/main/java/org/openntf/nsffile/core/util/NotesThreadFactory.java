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
package org.openntf.nsffile.core.util;

import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hcl.domino.DominoClient;
import com.hcl.domino.DominoClientBuilder;
import com.hcl.domino.DominoProcess;
import com.hcl.domino.misc.JNXThread;

/**
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NotesThreadFactory implements ThreadFactory {
	private static final Logger log = Logger.getLogger(NotesThreadFactory.class.getPackage().getName());
	
	public static final NotesThreadFactory instance = new NotesThreadFactory();
	public static final ExecutorService executor = Executors.newCachedThreadPool(instance);
	public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5, instance);
	
	@FunctionalInterface
	public static interface NotesFunction<T> {
		T apply(DominoClient client) throws Exception;
	}
	
	@FunctionalInterface
	public static interface NotesConsumer {
		void accept(DominoClient client) throws Exception;
	}
	
	/**
	 * Evaluates the provided function in a separate {@link NotesThread} with
	 * a {@link Session} for the active Notes ID.
	 * 
	 * @param <T> the type of object returned by {@code func}
	 * @param func the function to call
	 * @return the return value of {@code func}
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static <T> T call(NotesFunction<T> func) {
		try {
			return NotesThreadFactory.executor.submit(() -> {
				try(DominoClient client = DominoClientBuilder.newDominoClient().asIDUser().build()) {
					return func.apply(client);
				} catch(Throwable t) {
					if(log.isLoggable(Level.SEVERE)) {
						log.log(Level.SEVERE, "Encountered exception calling a NotesFunction", t);
					}
					throw t;
				}
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Evaluates the provided consumer in a separate {@link NotesThread} with
	 * a {@link Session} for the active Notes ID.
	 * 
	 * @param func the consumer to call
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static void run(NotesConsumer func) {
		call(client -> {
			func.accept(client);
			return null;
		});
	}
	
	/**
	 * Evaluates the provided function in a separate {@link NotesThread} with
	 * a {@link Session} for the provided Notes user name.
	 * 
	 * @param <T> the type of object returned by {@code func}
	 * @param userName the user to run the provided function as
	 * @param func the function to call
	 * @return the return value of {@code func}
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static <T> T callAs(String userName, NotesFunction<T> func) {
		try {
			return NotesThreadFactory.executor.submit(() -> {
				try(DominoClient client = DominoClientBuilder.newDominoClient().asUser(userName).build()) {
					return func.apply(client);
				} catch(Throwable t) {
					if(log.isLoggable(Level.SEVERE)) {
						log.log(Level.SEVERE, MessageFormat.format("Encountered exception calling a NotesFunction as {0}", userName), t);
					}
					throw t;
				}
			}).get();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Evaluates the provided consumer in a separate {@link NotesThread} with
	 * a {@link Session} for the provided Notes user name.
	 * 
	 * @param userName the user to run the provided function as
	 * @param func the consumer to call
	 * @throws RuntimeException wrapping any exception thrown by the main body
	 */
	public static void runAs(String userName, NotesConsumer func) {
		callAs(userName, client -> {
			func.accept(client);
			return null;
		});
	}

	@Override
	public Thread newThread(Runnable r) {
		return new JNXThread(r);
	}

	public static void term() {
		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.MINUTES);
		} catch(InterruptedException e) {
		}
		scheduler.shutdown();
		try {
			scheduler.awaitTermination(1, TimeUnit.MINUTES);
		} catch(InterruptedException e) {
		}
		
		NSFFileUtil.deleteTempFiles();
		
		DominoProcess.get().terminateProcess();
	}
}
