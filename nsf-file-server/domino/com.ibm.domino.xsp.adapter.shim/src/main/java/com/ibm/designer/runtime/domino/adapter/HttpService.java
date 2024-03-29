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
