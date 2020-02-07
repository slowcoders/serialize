package org.slowcoders.json;

import java.io.IOException;
import java.io.Reader;

import org.slowcoders.io.util.CharArrayWriterNTS;
import org.slowcoders.io.util.LexicalScanner;
import org.slowcoders.io.util.SourceReader;

public class JSONScanner extends LexicalScanner {

	CharArrayWriterNTS outBuff = new CharArrayWriterNTS();
	private boolean inQuoteScanning;
	private boolean isRawInput;

	// JJ JSONReader 에서 사용하기 위해 public 으로 변경
	public JSONScanner(Reader in) {
		super(in);
		super.setOptionFlags(OPTION_DUMP_SOURCE);
	}


	public final void scanQuotation(char delimiter, CharArrayWriterNTS outBuff)
			throws IOException {
		inQuoteScanning = true;
		int ch = this.read();
		loop : for (; ch != delimiter; ch = read()) {
			switch (ch) {
				case -1 :
					this.pushBack(ch);
					// css 끝에 있는 문자열은 delimiter 를 찾지 못해도 인정. (new-line 은 안됨)
					// www.w3.org/Style/CSS/Test/CSS2.1/20100316/html4/eof-003.htm
					break loop;
				case '\n' :
					this.throwSyntaxError("text line break");
				case '\n' | SourceReader.UNESCAPED_FLAG :
					if (delimiter == '\\' || delimiter == '\"') {
						/** html 과 css 모두 Quote 안에서만 \\ + \n 을 무시하여 처리한다. **/
						continue;
					}
			}

			if (outBuff != null) {
				outBuff.write(ch);
			}
		}
		inQuoteScanning = false;
	}


	public final String readQuotedText(char quote) throws IOException {
		outBuff.setSize(0);
		scanQuotation((char) quote, outBuff);
		return outBuff.reset();
	}


	@Override
    public final int getCharType(int ch) {
        int idxType;
        if (ch >= 0x80) {
        	// CSS Spec 상 0x80~0x100 사이의 문자는 identifier로 사용될 수 없다.
        	// 다만, CSS charset이 잘못 지정된 경우, Chrome은 이를 무시하고 허용한다.  
            return NON_ASCII;
        }
        else if (ch < 0) {
        	return 0;
        }
        return aType[ch];
    }

	public final void setRawInputMode(boolean isRawInput) {
		this.isRawInput = isRawInput;
	}

	@Override
	public boolean isEscapeChar(int ch) {
		if (isRawInput) {
			return false;
		}
		return ch == '\\' || (!this.inQuoteScanning && (ch == '#' || ch == '/'));
	}
	
	@Override
    protected int readEscapedChar(int ch) throws IOException {
		
		while (true) {
			if (ch == '\\') {
				return readControlChar();
			} 
		
			if (ch == '#') {
		        /*
		         * Skip a # hash end-of-line comment. The JSON RFC doesn't
		         * specify this behavior, but it's required to parse
		         * existing documents. See http://b/2571423.
		         */
		        super.scanUntil('\n', null);
				super.skipChar('\r');
				ch = super.read();
				continue;
			}
			
			if (ch != '/') {
				break;
			}
			
			ch = this.peekChar();
			if (ch == '*') {
				super.skipChar();
				int prev_ch = -1;
				// 주의) 모든 escape을 무시한다. So, read_raw(=super.read)를 호출한다.
				while ((ch = super.read()) != '/' || prev_ch != '*') {
					if (ch < 0) {
						return -1;
					}
					prev_ch = ch;
				}
				ch = super.read();
			}
			else if (ch == '/') {
				super.scanUntil('\n', null);
				super.skipChar('\r');
				ch = super.read();
			}
			else {
				break;
			}
		}
		return ch;
	}
	
	private int readControlChar() throws IOException {
    	while (true) { // loop가 아님.
    		int ch = super.read();
			switch (ch) {
				case 'b': // Backspace
					ch = '\b';
					break;
				case 'f': // Formfeed
					ch = '\f';
					break;
				case 'n': // New Line
					ch = '\n';
					break;
				case 'r': // Carriage return
					ch = '\r';
					break;
				case 't': // Tab
					ch = '\t';
					break;
				case '\n':
					// new-line 을 무시한다.
					super.skipChar('\r');
					ch = super.read();
					if (ch == '\\') {
						continue;
					}
					break;
				case 'u':
					ch = (int)super.readHexaDecimal(4, 4);
					break;
				case '\'': case '"':
					ch |= SourceReader.UNESCAPED_FLAG;
					break;
				default:
					break;
			}
			return ch;
		}
	}


	private static final int DIGIT = NUMBER_CHAR | HEXA_DECIMAL | NAME_CHAR;
	private static final int ALPHABET = ALPHABET_CHAR | NAME_CHAR | NAME_START;
	private static final int SPECIAL = SPECIAL_CHAR;
	private static final int CONTROL = CONTROL_CHAR;

	static int[] aType = {
			CONTROL, // 0
			CONTROL, // 1
			CONTROL, // 2
			CONTROL, // 3
			CONTROL, // 4
			CONTROL, // 5
			CONTROL, // 6
			CONTROL, // 7
			CONTROL, // 8
			WHITESPACE, // 9
			WHITESPACE, // a
			CONTROL, // b
			WHITESPACE, // c
			WHITESPACE, // d
			CONTROL, // e
			CONTROL, // f

			CONTROL, // 10
			CONTROL, // 11
			CONTROL, // 12
			CONTROL, // 13
			CONTROL, // 14
			CONTROL, // 15
			CONTROL, // 16
			CONTROL, // 17
			CONTROL, // 18
			CONTROL, // 19
			CONTROL, // 1a
			CONTROL, // 1b
			CONTROL, // 1c
			CONTROL, // 1d
			CONTROL, // 1e
			CONTROL, // 1f

			WHITESPACE, // ' ' 0x20
			SPECIAL, // '!'
			SPECIAL, // '"'
			SPECIAL, // '#'
			SPECIAL, // '$'
			SPECIAL, // '%'
			SPECIAL, // '&'
			SPECIAL, // '''

			SPECIAL, // '('
			SPECIAL, // ')'
			NAME_START, // '*'
			SPECIAL, // '+'
			SPECIAL, // ','
			NAME_CHAR | NAME_START, // '-'
			SPECIAL, // '.'
			SPECIAL, // '/'

			DIGIT, // '0' 0x30
			DIGIT, // '1'
			DIGIT, // '2'
			DIGIT, // '3'
			DIGIT, // '4'
			DIGIT, // '5'
			DIGIT, // '6'
			DIGIT, // '7'
			DIGIT, // '8'
			DIGIT, // '9'
			SPECIAL, // ':'
			SPECIAL, // ';'
			SPECIAL, // '<'
			SPECIAL, // '='
			SPECIAL, // '>'
			SPECIAL, // '?'

			SPECIAL, // '@' 0x40
			ALPHABET | HEXA_DECIMAL, // 'A'
			ALPHABET | HEXA_DECIMAL, // 'B'
			ALPHABET | HEXA_DECIMAL, // 'C'
			ALPHABET | HEXA_DECIMAL, // 'D'
			ALPHABET | HEXA_DECIMAL, // 'E'
			ALPHABET | HEXA_DECIMAL, // 'F'
			ALPHABET, // 'G'

			ALPHABET, // 'H'
			ALPHABET, // 'I'
			ALPHABET, // 'J'
			ALPHABET, // 'K'
			ALPHABET, // 'L'
			ALPHABET, // 'M'
			ALPHABET, // 'N'
			ALPHABET, // 'O'

			ALPHABET, // 'P' 0x50
			ALPHABET, // 'Q'
			ALPHABET, // 'R'
			ALPHABET, // 'S'
			ALPHABET, // 'T'
			ALPHABET, // 'U'
			ALPHABET, // 'V'
			ALPHABET, // 'W'
			ALPHABET, // 'X'
			ALPHABET, // 'Y'
			ALPHABET, // 'Z'
			SPECIAL, // '['
			SPECIAL, // '\\'
			SPECIAL, // ']'
			SPECIAL, // '^'
			ALPHABET, // '_'

			SPECIAL, // '`' 0x60
			ALPHABET | HEXA_DECIMAL, // 'a'
			ALPHABET | HEXA_DECIMAL, // 'b'
			ALPHABET | HEXA_DECIMAL, // 'c'
			ALPHABET | HEXA_DECIMAL, // 'd'
			ALPHABET | HEXA_DECIMAL, // 'e'
			ALPHABET | HEXA_DECIMAL, // 'f'
			ALPHABET, // 'g'
			ALPHABET, // 'h'
			ALPHABET, // 'i'
			ALPHABET, // 'j'
			ALPHABET, // 'k'
			ALPHABET, // 'l'
			ALPHABET, // 'm'
			ALPHABET, // 'n'
			ALPHABET, // 'o'

			ALPHABET, // 'p' 0x70
			ALPHABET, // 'q'
			ALPHABET, // 'r'
			ALPHABET, // 's'
			ALPHABET, // 't'
			ALPHABET, // 'u'
			ALPHABET, // 'v'
			ALPHABET, // 'w'
			ALPHABET, // 'x'
			ALPHABET, // 'y'
			ALPHABET, // 'z'
			SPECIAL, // '{'
			SPECIAL, // '|'
			SPECIAL, // '}'
			SPECIAL, // '~'
			SPECIAL, // 0x7F
	};

}
