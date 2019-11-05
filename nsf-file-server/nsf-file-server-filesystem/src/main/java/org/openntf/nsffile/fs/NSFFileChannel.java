package org.openntf.nsffile.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.openntf.nsffile.util.NotesThreadFactory;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.RichTextItem;

/**
 * 
 * @author Jesse Gallagher
 * @since 1.0.0
 */
public class NSFFileChannel extends FileChannel {
	
	private final NSFFileSystemProvider provider;
	private final NSFPath path;
	private Path tempFile;
	
	public NSFFileChannel(NSFFileSystemProvider provider, NSFPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
		this.provider = provider;
		this.path = path;
		
		try {
			this.tempFile = NotesThreadFactory.executor.submit(() -> {
				Document doc = provider.getDocument(path);
				Path result = Files.createTempFile(path.getFileName().toString(), ".tmp");
				if(doc.hasItem("File")) {
					// TODO add sanity checks
					RichTextItem rtitem = (RichTextItem)doc.getFirstItem("File");
					EmbeddedObject eo = (EmbeddedObject) rtitem.getEmbeddedObjects().get(0);
					try(InputStream is = eo.getInputStream()) {
						Files.copy(is, result, StandardCopyOption.REPLACE_EXISTING);
					}
				}
				
				return result;
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return getTempFileChannel().read(dst);
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		return getTempFileChannel().read(dsts, offset, length);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return getTempFileChannel().write(src);
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		return getTempFileChannel().write(srcs, offset, length);
	}

	@Override
	public long position() throws IOException {
		return getTempFileChannel().position();
	}

	@Override
	public FileChannel position(long newPosition) throws IOException {
		getTempFileChannel().position(newPosition);
		return this;
	}

	@Override
	public long size() throws IOException {
		return getTempFileChannel().size();
	}

	@Override
	public FileChannel truncate(long size) throws IOException {
		getTempFileChannel().truncate(size);
		return this;
	}

	@Override
	public void force(boolean metaData) throws IOException {
		// TODO ???
		getTempFileChannel().force(metaData);
	}

	@Override
	public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
		return getTempFileChannel().transferTo(position, count, target);
	}

	@Override
	public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
		return getTempFileChannel().transferFrom(src, position, count);
	}

	@Override
	public int read(ByteBuffer dst, long position) throws IOException {
		return getTempFileChannel().read(dst, position);
	}

	@Override
	public int write(ByteBuffer src, long position) throws IOException {
		return getTempFileChannel().write(src, position);
	}

	@Override
	public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
		return getTempFileChannel().map(mode, position, size);
	}

	@Override
	public FileLock lock(long position, long size, boolean shared) throws IOException {
		// TODO implement doc locking
		return getTempFileChannel().lock(position, size, shared);
	}

	@Override
	public FileLock tryLock(long position, long size, boolean shared) throws IOException {
		// TODO implement doc locking
		return getTempFileChannel().tryLock(position, size, shared);
	}

	@Override
	protected void implCloseChannel() throws IOException {
		// TODO Save back to the DB if updated
		getTempFileChannel().close();
	}

	// *******************************************************************************
	// * Internal utility methods
	// *******************************************************************************
	
	private FileChannel tempFileChannel;
	
	private synchronized FileChannel getTempFileChannel() throws IOException {
		if(this.tempFileChannel == null) {
			// TODO pass through options
			this.tempFileChannel = FileChannel.open(this.tempFile);
		}
		return this.tempFileChannel;
	}
}
