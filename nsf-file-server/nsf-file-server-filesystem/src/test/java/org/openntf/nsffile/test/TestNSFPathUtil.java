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
package org.openntf.nsffile.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.fs.util.NSFPathUtil;

public class TestNSFPathUtil {
	@Test
	public void testExtractAPIPath_local() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + NSFPathUtil.LOCAL_SERVER + "/foo.nsf/bar/baz");
		assertEquals("foo.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPath_local2() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + NSFPathUtil.LOCAL_SERVER + "/foo/bar.nsf/bar/baz");
		assertEquals("foo/bar.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPathRemote() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://someserver/foo.nsf/bar/baz");
		assertEquals("someserver!!foo.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPathRemote2() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://someserver/foo/bar.nsf/bar/baz");
		assertEquals("someserver!!foo/bar.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPathRemote3() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://someserver" + NSFPathUtil.NAME_DELIM + "SomeOrg/foo/bar.nsf/bar/baz");
		assertEquals("someserver/SomeOrg!!foo/bar.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testInvalidAPIPath() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://someserver/foo/bar/bar/baz");
		assertThrows(IllegalArgumentException.class, () -> NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractFilePath_local() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + ":///foo.nsf/bar/baz");
		assertEquals("/bar/baz", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testExtractFilePath_local2() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + ":///foo/bar.nsf/bar/baz");
		assertEquals("/bar/baz", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testExtractFilePathRemote() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://someserver/foo.nsf/bar/baz");
		assertEquals("/bar/baz", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testExtractFilePathRemote2() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://someserver/foo/bar.nsf/bar/baz");
		assertEquals("/bar/baz", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testInvalidFilePath() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://someserver/foo/bar/bar/baz");
		assertThrows(IllegalArgumentException.class, () -> NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testBasicURIConversion() throws URISyntaxException {
		String apiPath = "foo.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + NSFPathUtil.LOCAL_SERVER + "/foo.nsf");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testBasicURIConversion2() throws URISyntaxException {
		String apiPath = "foo/bar.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + NSFPathUtil.LOCAL_SERVER + "/foo/bar.nsf");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversion() throws URISyntaxException {
		String apiPath = "someserver!!foo.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@someserver/foo.nsf");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversion2() throws URISyntaxException {
		String apiPath = "someserver!!foo/bar.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@someserver/foo/bar.nsf");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversion3() throws URISyntaxException {
		String apiPath = "someserver/SomeOrg!!foo/bar.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@someserver" + NSFPathUtil.NAME_DELIM + "SomeOrg/foo/bar.nsf");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversion4() throws URISyntaxException {
		String apiPath = "some.server.com/SomeOrg!!foo/bar.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@some.server.com" + NSFPathUtil.NAME_DELIM + "SomeOrg/foo/bar.nsf");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversionPath() throws URISyntaxException {
		String apiPath = "some.server.com/SomeOrg!!foo/bar.nsf";
		String filePath = "foo";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@some.server.com" + NSFPathUtil.NAME_DELIM + "SomeOrg/foo/bar.nsf/foo");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath, filePath));
	}
	
	@Test
	public void testRemoteURIConversionPath2() throws URISyntaxException {
		String apiPath = "some.server.com/SomeOrg!!foo/bar.nsf";
		String filePath = "foo/bar";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@some.server.com" + NSFPathUtil.NAME_DELIM + "SomeOrg/foo/bar.nsf/foo/bar");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath, filePath));
	}
	
	@Test
	public void testRemoteURIConversionPath3() throws URISyntaxException {
		String apiPath = "some.server.com/SomeOrg!!foo/bar.nsf";
		String filePath = "foo/bar";
		String more = "baz";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@some.server.com" + NSFPathUtil.NAME_DELIM + "SomeOrg/foo/bar.nsf/foo/bar/baz");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath, filePath, more));
	}
}
