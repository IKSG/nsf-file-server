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
package com.ibm.designer.runtime.domino.adapter;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletRequestAdapter;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletResponseAdapter;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpSessionAdapter;

public abstract class HttpService {
	public HttpService(LCDEnvironment env) {
		
	}
	
	public void destroyService() {
		
	}
	
	public abstract boolean doService(String contextPath, String path, HttpSessionAdapter httpSession, HttpServletRequestAdapter httpRequest,
			HttpServletResponseAdapter httpResponse) throws ServletException, IOException;

	public abstract void getModules(List<ComponentModule> modules);
}
