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

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.openntf.nsffile.fs.util.NSFPathUtil;

@SuppressWarnings("nls")
public class TestNSFPathUtil {
	@Test
	public void testUriRoundTrip() throws URISyntaxException {
		String apiPath = "foo.nsf";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTrip2() throws URISyntaxException {
		String apiPath = "foo/bar.nsf";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTrip3() throws URISyntaxException {
		String apiPath = "852584A8:00507284";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTrip4() throws URISyntaxException {
		String apiPath = "852584A800507284";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServer() throws URISyntaxException {
		String apiPath = "someserver!!foo.nsf";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServer2() throws URISyntaxException {
		String apiPath = "someserver!!foo/bar.nsf";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServer3() throws URISyntaxException {
		String apiPath = "someserver!!852584A8:00507284";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServer4() throws URISyntaxException {
		String apiPath = "someserver!!852584A800507284";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServerDn() throws URISyntaxException {
		String apiPath = "CN=some.server/O=SomeOrg!!foo.nsf";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServerDn2() throws URISyntaxException {
		String apiPath = "CN=some.server/O=SomeOrg!!foo/bar.nsf";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServerDn3() throws URISyntaxException {
		String apiPath = "CN=some.server/O=SomeOrg!!852584A8:00507284";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServerDn4() throws URISyntaxException {
		String apiPath = "CN=some.server/O=SomeOrg!!852584A800507284";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath);
		assertEquals(apiPath, NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testUriRoundTripServerDnFilePath() throws URISyntaxException {
		String apiPath = "CN=some.server/O=SomeOrg!!foo.nsf";
		String filePath = "/foo/bar";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath, filePath);
		assertEquals(filePath, NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testUriRoundTripServerDnFilePath2() throws URISyntaxException {
		String apiPath = "CN=some.server/O=SomeOrg!!foo/bar.nsf";
		String filePath = "/foo/bar";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath, filePath);
		assertEquals(filePath, NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testUriRoundTripServerDnFilePath3() throws URISyntaxException {
		String apiPath = "CN=some.server/O=SomeOrg!!852584A8:00507284";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath, "foo", "bar");
		assertEquals("/foo/bar", NSFPathUtil.extractPathInfo(uri));
	}
	
	@Test
	public void testUriRoundTripServerDnFilePath4() throws URISyntaxException {
		String apiPath = "CN=some.server/O=SomeOrg!!852584A800507284";
		URI uri = NSFPathUtil.toFileSystemURI(null, apiPath, "foo", "bar");
		assertEquals("/foo/bar", NSFPathUtil.extractPathInfo(uri));
	}
}
