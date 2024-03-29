package com.ibm.designer.runtime.domino.adapter;

public interface IServiceFactory {
	HttpService[] getServices(LCDEnvironment env);
}
