package org.slowcoders.io.util;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: InterWise</p>
 * @author unascribed
 * @version 1.0
 */

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.InputStreamReader;
import java.io.IOException;


//import java.util.Locale;
import java.util.Vector;
import java.util.Stack;
import java.util.EmptyStackException;

//import org.ninefolders.util.NPDebug;
import org.xml.sax.Parser;
import org.xml.sax.DocumentHandler;

import org.xml.sax.EntityResolver;
import org.xml.sax.DTDHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.AttributeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/*
 Not-Thread-Safe Character-Array Writer.
*/
public class CharArrayWriterNTS { //extends Writer {

    protected char charBuff[];
    protected int charCount;

	public CharArrayWriterNTS() {
		charBuff = new char[256];
	}

    public void write(int ch) {
        ensureCapacity(1);
        charBuff[charCount++] = (char)ch;
    }

    public String reset() {
        String str = new String(charBuff, 0, charCount);
        //return str;
    	//String s = this.toString();
        charCount = 0;
        return str;
    }

    public void close() {
		charCount = 0;
    }

    public void flush() {
		charCount = 0;
    }

    public final void write(char[] buff, int offset, int length) {
        ensureCapacity(length);
        System.arraycopy(buff, offset, charBuff, charCount, length);
        charCount += length;
    }

    public final void write(String str, int offset, int length) {
        ensureCapacity(length);
        str.getChars(offset, length, charBuff, charCount);
        charCount += length;
    }

    public final int size() {
        return charCount;
    }

    public final int charAt(int idx) {
        return this.charBuff[idx];
    }

    public final void setSize(int size) {
        this.charCount = size;
    }

    public String toString() {
    	throw new RuntimeException("shouldNotBeHere");
        //String str = new String(charBuff, 0, charCount);
        //return str;
    }

    public final char[] toCharArray() {
        char[] str = new char[charCount];
        System.arraycopy(charBuff, 0, str, 0, charCount);
        return str;
    }

    private void ensureCapacity(int length) {
        int newCount = charCount + length;
        if (newCount > charBuff.length) {
            if (newCount > 64*1024) {
                int a = 3;
            }
            char newbuf[] = new char[Math.max(charBuff.length << 1, newCount)];
            System.arraycopy(charBuff, 0, newbuf, 0, charCount);
            charBuff = newbuf;
        }
    }

    public int lastIndexOf(int ch) {
         return lastIndexOf(ch, this.charCount - 1);
    }

    public int lastIndexOf(int ch, int fromIndex) {
        for (int i = fromIndex; i > 0; i --) {
            if (charBuff[i] == ch) {
                return i;
            }
        }
        return -1;
    }

	public String substring(int offset) {
		String s = new String(charBuff, offset, charCount-offset);
		return s;
	}

}
