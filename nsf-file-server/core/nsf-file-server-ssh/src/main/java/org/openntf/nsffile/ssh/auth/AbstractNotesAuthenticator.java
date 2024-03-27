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
package org.openntf.nsffile.ssh.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.ibm.commons.util.StringUtil;

import org.openntf.nsffile.core.util.NSFFileUtil;

import lotus.domino.Directory;
import lotus.domino.DirectoryNavigator;
import lotus.domino.NotesException;
import lotus.domino.Session;

public abstract class AbstractNotesAuthenticator {

	/**
	 * Does some basic sanitizing on value to allow it to be inserted into a double-quoted string inside a Notes formula. 
	 */
	protected String escapeForFormulaString(String value) {
		if(StringUtil.isEmpty(value)) {
			return ""; //$NON-NLS-1$
		} else {
			return value.replace("\\", "\\\\").replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	protected List<String> getItemValueStringListForUser(Session session, String dominoName, String itemName) throws NotesException {
		if(StringUtil.isEmpty(dominoName)) {
			return Collections.emptyList();
		} else {
			Directory dir = session.getDirectory();
			DirectoryNavigator nav = dir.lookupNames("$Users", new Vector<String>(Arrays.asList(dominoName)), new Vector<String>(Arrays.asList(itemName)), false); //$NON-NLS-1$
			if(nav.findFirstMatch()) {
				List<?> itemValue = nav.getFirstItemValue();

				return NSFFileUtil.toStringList(itemValue);
			} else {
				return Collections.emptyList();
			}
		}
	}

	protected String getItemValueStringForUser(Session session, String dominoName, String itemName) throws NotesException {
		if(StringUtil.isEmpty(dominoName)) {
			return ""; //$NON-NLS-1$
		} else {
			Directory dir = session.getDirectory();
			DirectoryNavigator nav = dir.lookupNames("$Users", new Vector<String>(Arrays.asList(dominoName)), new Vector<String>(Arrays.asList(itemName)), false); //$NON-NLS-1$
			if(nav.findFirstMatch()) {
				List<?> itemValue = nav.getFirstItemValue();

				return StringUtil.toString(itemValue.get(0));
			} else {
				return ""; //$NON-NLS-1$
			}
		}
	}

}
