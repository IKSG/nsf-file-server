package org.openntf.nsffile.util;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;

import com.ibm.domino.napi.c.NotesUtil;
import com.ibm.domino.napi.c.xsp.XSPNative;

import lotus.domino.Session;

public class SudoUtils {

	/**
	 * @param userName
	 *            the user name to create the session as
	 * @param handleTracker
	 *            a collection to hold the allocated Domino handle, for later
	 *            discarding
	 * @return the created session
	 */
	public static Session getSessionAs(final String userName, final Collection<Long> handleTracker) {
		Session result = null;
		try {
			result = AccessController.doPrivileged((PrivilegedExceptionAction<Session>) () -> {
				long hList = NotesUtil.createUserNameList(userName);
				if (handleTracker != null) {
					handleTracker.add(hList);
				}
				return XSPNative.createXPageSession(userName, hList, true, false);
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}

