package org.openntf.nsffile.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.openntf.nsffile.fs.NSFFileSystemProvider;
import org.openntf.nsffile.uril.NSFPathUtil;

public class TestNSFPathUtil {
	@Test
	public void testExtractAPIPathLocal() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + ":///foo.nsf/bar/baz");
		assertEquals("foo.nsf", NSFPathUtil.extractApiPath(uri));
	}
	
	@Test
	public void testExtractAPIPathLocal2() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + ":///foo/bar.nsf/bar/baz");
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
	public void testInvalidPath() {
		URI uri = URI.create(NSFFileSystemProvider.SCHEME + "://someserver/foo/bar/bar/baz");
		assertThrows(IllegalArgumentException.class, () -> NSFPathUtil.extractApiPath(uri));
	}
}
