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

//import com.wise.cldc.io.*;
//import com.wise.util.*;


/**
 * readXXX() 명령에 의해 읽혀진 마지막 문자는 Buffer에 기록되지 않는다.
 * 따라서, 마직막 읽힌 문자를 Buffer에 넣기 위해선 write()를 호출하여
 * 주어야 한다.
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: InterWise</p>
 * @author unascribed
 * @version 1.0
 */
public abstract class LexicalScanner extends SourceReader {

    // pre-defined character types.
    public static final int NAME_CHAR = 1;
    public static final int NAME_START = 2;
    public static final int HEXA_DECIMAL = 4;
    public static final int NUMBER_CHAR = 8;
	public static final int NON_ASCII = 16;
	public static final int ALPHABET_CHAR = 32;
	public static final int SPECIAL_CHAR = 64;
    public static final int WHITESPACE = 128;
    public static final int CONTROL_CHAR = 256;
    
    public static final int CUSTOM_CHAR = 512;

    private int pushBack = Integer.MIN_VALUE;
    
    private CharArrayWriterNTS tmpBuff = new CharArrayWriterNTS();
	private boolean isCaseSensitive;

    public LexicalScanner(Reader source)  {
    	super(source);
    }
    
	protected void setCaseSensitive(boolean isCaseSensitive) {
		this.isCaseSensitive = isCaseSensitive;
    }

	public boolean isCaseSesitive() {
		return isCaseSensitive;
	}

	public abstract int getCharType(int ch);

	public void throwSyntaxError(String msg) {
		throw new RuntimeException(msg);
	}


	public final int readPrintable() throws IOException {
		int ch = this.read();
		while (this.getCharType(ch) == WHITESPACE) {
			ch = read();
		}
		return ch;
	}

	public final int skipWhiteSpace() throws IOException {
		int ch = readPrintable();
		this.pushBack(ch);
		return ch;
	}

    public final boolean skipChar(int ch) throws IOException {
        int curr_ch = read();
        if (curr_ch != ch) {
        	this.pushBack(curr_ch);
        	return false;
        }
        return true;
    }
    
    public final int skipCharType(int charType) throws IOException {
	    int ch = this.read();
	    if ((getCharType(ch) & charType) != 0) {
	    	return ch;
	    }
		pushBack(ch);
		return Integer.MIN_VALUE/2;
    }
    
	public boolean trySkipAscii(String text, boolean caseSesitive) throws IOException {
		//int last_push = this.pushBack;
		this.mark(text.length());
		if (!caseSesitive) {
			text = text.toLowerCase();
		}
		for (int i = 0; i < text.length(); i ++) {
			int ch_ref = text.charAt(i);
			int ch_read = this.read();
			if (!caseSesitive && ch_read >= 'A' && ch_read <= 'Z') {
				ch_read |= 0x20;
			}
			if (ch_ref != ch_read) {
				this.reset();
				return false;
			}
		}
		this.mark(-1);
		return true;
	}

    
    
    public final void assertNextChar(int ch) throws IOException {
        int curr_ch = read();
        assertMatch(curr_ch, ch);
    }

    public final void assertNextPrintable(int ch) throws IOException {
        int curr_ch = readPrintable();
        assertMatch(curr_ch, ch);
    }

    public final void assertNextString(String str) throws IOException {
        for (int i = 0; i < str.length(); i ++) {
            assertNextChar(str.charAt(i));
        }
    }

    public final void assertNextStringIgnoreCase(String str) throws IOException {
        for (int i = 0; i < str.length(); i ++) {
            int curr_ch = read();
            if (curr_ch >= 'A' && curr_ch <= 'Z') {
                curr_ch += 'a' - 'A';
            }
            assertMatch(curr_ch, str.charAt(i));
        }
    }

    private void assertMatch(int curr_char, int expected_char) throws IOException {
        if (curr_char != expected_char) {
            throwSyntaxError("expected character is [" + (char)expected_char +
                       "] but current character is [" + (char)curr_char + " = 0x"
                       + Integer.toHexString(curr_char) + "]");
        }
    }

    public final int scanWhile(int charType, CharArrayWriterNTS outBuff) throws IOException {
        int ch = this.read();
        while ((getCharType(ch) & charType) != 0) {
            if (outBuff != null) {
                outBuff.write(ch);
            }
            ch = read();
        }
		pushBack(ch);
		return ch;
    }

    public final int scanLetters(CharArrayWriterNTS outBuff) throws IOException {
        int ch = this.read();
        while (Character.isLetter(ch)) {
            if (outBuff != null) {
                outBuff.write(ch);
            }
            ch = read();
        }
		pushBack(ch);
		return ch;
    }

    public final int skipAny(char[] codeset) throws IOException {
        int ch = this.read();
        for (char code : codeset) {
        	if (ch == code) {
        		return ch;
        	}
        }
        this.pushBack(ch);
        return Integer.MIN_VALUE/2;
    }
    
    public final int scanUntil(char[] aDelimiter, CharArrayWriterNTS outBuff) throws IOException {
        int cntDelimiter = aDelimiter.length;
        int ch = this.read();
        loop:
        while (ch >= 0) {
            for (int i = cntDelimiter; i -- > 0; ) {
                if (ch == aDelimiter[i]) {
                    break loop;
                }
            }
            if (outBuff != null) {
                outBuff.write((char)ch);
            }
            ch = read();
        }
        return ch;
    }

    public final int scanPrintableUntil(char[] aDelimiter, CharArrayWriterNTS outBuff) throws IOException {
        int cntDelimiter = aDelimiter.length;
        int ch = this.read();
        loop:
        while (ch > ' ') {
            for (int i = cntDelimiter; i -- > 0; ) {
                if (ch == aDelimiter[i]) {
                	break loop;
                }
            }
            if (outBuff != null) {
                outBuff.write(ch);
            }
            ch = read();
        }
        return ch;
    }
    
    public final int scanUntil(char delimiter, CharArrayWriterNTS outBuff) throws IOException {
        int ch = this.read();
        while (ch != delimiter) {
        	if (ch < 0) {
        		break;
        	}
            if (outBuff != null) {
                outBuff.write(ch);
            }
            ch = read();
        }
        return ch;
    }

    public final boolean scanUntil(PlainPattern finder, CharArrayWriterNTS outBuff) throws IOException {
        int idx = 0;
        int ch = this.read();
        while (ch >= 0) {
            idx = finder.next(idx, ch);
            if (idx < 0) {
                if (outBuff != null) {
                	outBuff.setSize(outBuff.size() - finder.getTokenLength() + 1);
                }
                return true;
            }
            if (outBuff != null) {
                outBuff.write(ch);
            }
            ch = read();
        }
        return false;
    }

    public final int scanIdentifier(boolean caseSensitive, CharArrayWriterNTS buff) throws IOException {
        int ch = scanIdentifierInternal(caseSensitive, buff);
        return ch;
    }

    private final int scanIdentifierInternal(boolean caseSensitive, CharArrayWriterNTS outBuff) throws IOException {
        int ch = this.read();
        int char_type = getCharType(ch);
        if ((char_type & NAME_START) == 0) {
        		this.pushBack(ch);
            return ch | Integer.MIN_VALUE;
        }
        if (ch == '-') {
            ch = read();
            if (ch >= '0' && ch <= '9') {
            	// 숫자인 경우엔 오류 발생!
                return ch | Integer.MIN_VALUE;
            }
            if (outBuff != null) {
            	outBuff.write('-');
            }
        }

        while (true) {
            if (outBuff != null) {
                if (!caseSensitive && (char)ch >= 'A' && (char)ch <= 'Z') {
                	// escaped된 대문자를 escaped된 소문자로 바꾸기 위해 (char)형으로 casting
                	// 영문에 한해서만 대소문자 구분을 무시한다.
                    ch |= 0x20;
                }
                outBuff.write(ch);
            }
            ch = read();
            if ((getCharType(ch) & NAME_CHAR) == 0) {
                break;
            }
        }
		this.pushBack(ch);
        return ch;
    }    
    
    public final String readIdentifier() throws IOException {
    	    tmpBuff.setSize(0);
        scanIdentifier(this.isCaseSensitive, tmpBuff);
        return tmpBuff.reset();
    }

    public final long readDigit() throws IOException {
        return readInteger(10);
    }

    public final long readOctalDigit() throws IOException {
        return readInteger(8);
    }

    public final long readInteger(int radix) throws IOException {
    	if (radix > 10) {
    		if (radix != 16) {
    			throw new RuntimeException("invalid radix");
    		}
    		return readHexaDecimal(1, 16);
    	}
        int last_digit = '0' + radix - 1;

        int ch = this.read();
        if (ch < '0' || ch > last_digit) {
            throwSyntaxError("not a digit");
        }

        long v = ch - '0';
        boolean digitFound = false;
        while (true) {
            ch = read();
            if (ch < '0' || ch > last_digit) {
                this.pushBack(ch);
                return v;
            }
            v = v * radix + (ch - '0');
        }
    }

    public final long readHexaDecimal(int min_len, int max_len) throws IOException {
        int ch = this.peekChar();
        if ((getCharType(ch) & HEXA_DECIMAL) == 0) {
            throwSyntaxError("not a hexa-decimal digit " + (char)ch);
        }

        long v = 0;
        while (max_len -- > 0) {
            ch = read();

            if ((getCharType(ch) & HEXA_DECIMAL) == 0) {
                this.pushBack(ch);
            	if (min_len > 0) {
                    throwSyntaxError("too short hexa-decimal digit " + (char)ch);
            	}
                break;
            }

            min_len --;
			if (ch <= '9') {
				v = (v << 4) + (ch & 0xF);
			}
			else {
				v = (v << 4) + (ch & 0xF) + 9;
			}

        }
        return v;
    }


}
