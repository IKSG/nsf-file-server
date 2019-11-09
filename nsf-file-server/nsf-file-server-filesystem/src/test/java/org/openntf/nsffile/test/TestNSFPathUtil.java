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
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.fs.util.NSFPathUtil;

@SuppressWarnings("nls")
public class TestNSFPathUtil {
	private static final Function<String, String> encoder = NSFPathUtil.encoder;
	
	@Test
	public void testExtractAPIPath_local() {
		String host = encoder.apply(NSFPathUtil.LOCAL_SERVER);
		String nsf = encoder.apply("foo.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + host + "/" + nsf + "/bar/baz");
		assertEquals("foo.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPath_local2() {
		String host = encoder.apply(NSFPathUtil.LOCAL_SERVER);
		String nsf = encoder.apply("foo/bar.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + host + "/" + nsf + "/bar/baz");
		assertEquals("foo/bar.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPathRemote() {
		String host = encoder.apply("someserver");
		String nsf = encoder.apply("foo.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + host + "/" + nsf + "/bar/baz");
		assertEquals("someserver!!foo.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPathRemote2() {
		String host = encoder.apply("someserver");
		String nsf = encoder.apply("foo/bar.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + host + "/" + nsf + "/bar/baz");
		assertEquals("someserver!!foo/bar.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPathRemote3() {
		String host = encoder.apply("someserver/SomeOrg");
		String nsf = encoder.apply("foo/bar.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + host + "/" + nsf + "/bar/baz");
		assertEquals("someserver/SomeOrg!!foo/bar.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractFilePath_local() {
		String nsf = encoder.apply("foo.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + ":///" + nsf + "/bar/baz");
		assertEquals("/bar/baz", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testExtractFilePath_local2() {
		String nsf = encoder.apply("foo/bar.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + ":///" + nsf + "/bar/baz");
		assertEquals("/bar/baz", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testExtractFilePathRemote() {
		String host = encoder.apply("someserver");
		String nsf = encoder.apply("foo.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + host + "/" + nsf + "/bar/baz");
		assertEquals("/bar/baz", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testExtractFilePathRemote2() {
		String host = encoder.apply("someserver");
		String nsf = encoder.apply("foo/bar.nsf");
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://" + host + "/" + nsf + "/bar/baz");
		assertEquals("/bar/baz", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testBasicURIConversion() throws URISyntaxException {
		String host = encoder.apply(NSFPathUtil.LOCAL_SERVER);
		String nsf = encoder.apply("foo.nsf");
		String apiPath = "foo.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf);
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testBasicURIConversion2() throws URISyntaxException {
		String host = encoder.apply(NSFPathUtil.LOCAL_SERVER);
		String nsf = encoder.apply("foo/bar.nsf");
		String apiPath = "foo/bar.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf);
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversion() throws URISyntaxException {
		String host = encoder.apply("someserver");
		String nsf = encoder.apply("foo.nsf");
		String apiPath = "someserver!!foo.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf);
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversion2() throws URISyntaxException {
		String host = encoder.apply("someserver");
		String nsf = encoder.apply("foo/bar.nsf");
		String apiPath = "someserver!!foo/bar.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf);
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversion3() throws URISyntaxException {
		String host = encoder.apply("someserver/SomeOrg");
		String nsf = encoder.apply("foo/bar.nsf");
		String apiPath = "someserver/SomeOrg!!foo/bar.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf);
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversion4() throws URISyntaxException {
		String host = encoder.apply("some.server.com/SomeOrg");
		String nsf = encoder.apply("foo/bar.nsf");
		String apiPath = "some.server.com/SomeOrg!!foo/bar.nsf";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf);
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath));
	}
	
	@Test
	public void testRemoteURIConversionPath() throws URISyntaxException {
		String host = encoder.apply("some.server.com/SomeOrg");
		String nsf = encoder.apply("foo/bar.nsf");
		String apiPath = "some.server.com/SomeOrg!!foo/bar.nsf";
		String filePath = "foo";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf + "/foo");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath, filePath));
	}
	
	@Test
	public void testRemoteURIConversionPath2() throws URISyntaxException {
		String host = encoder.apply("some.server.com/SomeOrg");
		String nsf = encoder.apply("foo/bar.nsf");
		String apiPath = "some.server.com/SomeOrg!!foo/bar.nsf";
		String filePath = "foo/bar";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf + "/foo/bar");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath, filePath));
	}
	
	@Test
	public void testRemoteURIConversionPath3() throws URISyntaxException {
		String host = encoder.apply("some.server.com/SomeOrg");
		String nsf = encoder.apply("foo/bar.nsf");
		String apiPath = "some.server.com/SomeOrg!!foo/bar.nsf";
		String filePath = "foo/bar";
		String more = "baz";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf + "/foo/bar/baz");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath, filePath, more));
	}
	
	@Test
	public void testReplicaIDConversionPath() throws URISyntaxException {
		String host = encoder.apply("some.server.com/SomeOrg");
		String nsf = encoder.apply("852584A8:00507284");
		String apiPath = "some.server.com/SomeOrg!!852584A8:00507284";
		String filePath = "foo/bar";
		String more = "baz";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf + "/foo/bar/baz");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath, filePath, more));
	}
	
	@Test
	public void testReplicaIDConversionPath2() throws URISyntaxException {
		String host = encoder.apply("some.server.com/SomeOrg");
		String nsf = encoder.apply("852584A800507284");
		String apiPath = "some.server.com/SomeOrg!!852584A800507284";
		String filePath = "foo/bar";
		String more = "baz";
		URI expected = URI.create(NSFFileSystemProvider.SCHEME + "://John%20Doe@" + host + "/" + nsf + "/foo/bar/baz");
		assertEquals(expected, NSFPathUtil.toFileSystemURI("John Doe", apiPath, filePath, more));
	}
}
