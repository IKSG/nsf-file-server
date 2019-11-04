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
package org.openntf.nsffile.util;

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
