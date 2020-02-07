package org.slowcoders.io.util;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: InterWise</p>
 * @author unascribed
 * @version 1.0
 */

import org.slowcoders.util.Debug;

import java.io.*;
import java.nio.charset.Charset;

//import org.ninefolders.util.NPDebug;


public abstract class SourceReader {

	private static final int MAX_PUSHBACK = 16;
	private Reader in;
	private char char_buff[] = new char[8192 + MAX_PUSHBACK];
	private int buff_start, buff_end;
	private int currRow, currCol;
	private int cntPushback;
	private int markedPushbackCount;
	private int markedPos = -1;
	private int read_total;
	private int options;
	private boolean inReadEscapeChar;
	private static final int RAW_PUSHBACK  = 0x20000;
	public static final int UNESCAPED_FLAG = 0x10000;

	public static final int OPTION_DUMP_SOURCE = 1;

	public SourceReader(Reader source) {
		this.in = source;
	}

	protected void setInputSource(Reader source) throws IOException {
		this.close();
		this.currCol = currRow = 0;
		this.in = source;
		this.buff_start = MAX_PUSHBACK;
	}
	
	public final void setOptionFlags(int options) {
		this.options = options;
	}
	
	public final int getOptionFlags() {
		return this.options;
	}
	
	public final boolean isCharsetResolved() {
		return !(in instanceof DefaultCharsetReader);
	}

	public void close() throws IOException {
		Reader in = this.in;
		if (in != null) {
			this.buff_end = this.buff_start = MAX_PUSHBACK;
			this.markedPos = -1;
			this.in = null;
			in.close();
		}
	}

	public final boolean isClosed() throws IOException {
		return this.peekChar() < 0;
	}

	public void mark(int limit) {
		if (limit <= 0) {
			this.markedPos = -1;
			return;
		}
		int MAX_BUFF_LEN = char_buff.length - MAX_PUSHBACK;
		int offset = this.buff_start;
		int remain = char_buff.length - offset;
		if (remain < limit) {
			if (limit > MAX_BUFF_LEN) {
				throw new IllegalArgumentException("too big buffer size: "  + limit);
			}
			char buff[] = this.char_buff;
			int start = MAX_PUSHBACK - cntPushback;
			System.arraycopy(buff, offset, buff, start, this.buff_end - offset);
			this.buff_end -= offset - start;
			this.buff_start = offset = start;
		}
		this.markedPos = offset;
		this.markedPushbackCount = this.cntPushback;
	}

	public boolean reset() {
		if (this.markedPos >= 0) {
			this.buff_start = this.markedPos;
			this.cntPushback = this.markedPushbackCount;
			this.markedPos = -1;
			return true;
		}
		return false;
	}

	public final void pushBack(int ch) throws IOException {
		if (ch < 0) {
			return;
		}
		int offset = this.buff_start-1;
		if (this.markedPos >= 0) {
			if (char_buff[offset] != ch) {
				throw new IOException("Can not push back altered character while in marked mode");
			}
		}
		char_buff[this.buff_start = offset] = (char)ch;
		this.cntPushback ++;
	}

	public final int peekChar() throws IOException {
		if (this.buff_start >= this.buff_end) {
			if (!this.fill()) {
				return -1;
			}
		}
		int ch = this.char_buff[this.buff_start];

		if (this.cntPushback <= 0 && !inReadEscapeChar && isEscapeChar(ch)) {
			this.dumpInput(ch);
			this.buff_start ++;
			inReadEscapeChar = true;
			ch = this.readEscapedChar(ch);
			inReadEscapeChar = false;
			this.buff_start --;
		}
		return ch;
	}

	public final int read() throws IOException {
		int ch = peekChar();
		this.buff_start ++;
		if (this.cntPushback != 0) {
			Debug.Assert(this.cntPushback >= 0);
			this.cntPushback--;
			return ch;
		}

		dumpInput(ch);
		return ch;
	}

	private void dumpInput(int ch) {
		if ((this.options & OPTION_DUMP_SOURCE) != 0) {
			System.out.print((char) ch);
		}

		if (ch == '\n') {
			currRow++;
			currCol = 0;
		} else {
			currCol++;
		}
	}

	protected abstract boolean isEscapeChar(int ch);

	protected abstract int readEscapedChar(int escape_char) throws IOException;

	public final void skipChar() throws IOException {
		read();
	}

	private boolean fill() throws IOException {
		if (in == null) {
			return false;
		}
		assert(this.buff_start == this.buff_end);

		if (this.markedPos < 0) {
			this.buff_end = this.buff_start = MAX_PUSHBACK;
		} else if (markedPos <= MAX_PUSHBACK && this.buff_end >= char_buff.length) {
			this.char_buff = new char[this.char_buff.length * 2];
		} else {
			int m_pos = (int) this.markedPos;
			System.arraycopy(this.char_buff, m_pos, this.char_buff, MAX_PUSHBACK,
					this.buff_end - m_pos);
			this.buff_end -= m_pos;
			this.buff_start -= m_pos;
			this.markedPos = MAX_PUSHBACK;
		}

		int len = in.read(char_buff, this.buff_end,
				char_buff.length - this.buff_end);
		if (len > 0) {
			this.buff_end += len;
			read_total += len;

			return true;
		}
		if (len < 0) {
			in = null;
		}
		return false;
	}

	public final int getLineNumber() {
		return this.currRow;
	}

	public final int getColumnNumber() {
		return this.currCol;
	}

	public String toString() {
		return (this.currRow + 1) + "/" + (this.currCol + 1);
	}

	public void setCharSet(String charset) throws IOException {
		if (!(in instanceof DefaultCharsetReader)) {
			return;
		}
		DefaultCharsetReader ir = (DefaultCharsetReader) in;
		if (Charset.forName(charset) == Charset.defaultCharset()) {
			return;
		}
		
		InputStream ins = ir.getInputStream();
		ins.reset();
		this.in = new InputStreamReader(ins, charset);
		int char_len = this.buff_end - this.buff_start;
		char skip_buff[] = new char[2048];
		for (int skip_len = read_total - char_len; skip_len > 0;) {
			int len = in.read(skip_buff, 0, skip_len > skip_buff.length ? skip_buff.length : skip_len);
			skip_len -= len;
		}
		int len = in.read(this.char_buff, this.buff_start, char_len);
		this.buff_end = this.buff_start + len;
	}

	public static InputStreamReader createDefaultInputStreamReader(InputStream in) {
		return new DefaultCharsetReader(in);
	}
	
	private static class DefaultCharsetReader extends InputStreamReader {
	    InputStream in;

	    DefaultCharsetReader(InputStream in) {
			super(in);
			this.in = in;
		}

		final InputStream getInputStream() {
	        return in;
	    }
	}

}
