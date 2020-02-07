package org.slowcoders.io.util;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: InterWise</p>
 * @author unascribed
 * @version 1.0
 */

import java.io.*;

public class ChunkedInputStream extends FilterInputStream {

    int sizeChunk;
    int pos;

    public ChunkedInputStream(InputStream in, int size) {
        super(in);
        this.sizeChunk = size;
    }

    public final int getPosition() {
        return pos;
    }

    public int read() throws IOException {
        if (pos > sizeChunk) {
            throw new EOFException();
        }
        if (pos == sizeChunk) {
            sizeChunk = -1;
            return -1;
        }
        pos ++;
        return super.read();
    }

    public int read(byte[] bytes, int off, int length) throws IOException {
        if (pos > sizeChunk) {
            throw new EOFException();
        }
        if (pos + length > sizeChunk) {
            length = sizeChunk - pos;
            if (length == 0) {
                sizeChunk = -1;
                return -1;
            }
        }
        return super.read(bytes, off, length);
    }
}
