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


public final class PlainPattern {

    char[] token;
    short[] aRollback;
    boolean ignoreCase;

    public PlainPattern(String token, boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        if (ignoreCase) {
            token = token.toLowerCase();
        }
        init(token.toCharArray());
    }

//    public StringFinder(byte[] token, int offset, int length) {
//        char[] str = new char[length];
//        init(str);
//    }
//
//    public StringFinder(char[] token, int offset, int length) {
//        char[] str = new char[length];
//        System.arraycopy(token, offset, str, 0, length);
//        init(str);
//    }
//
//    public void setIgnoreCase(boolean ignoreCase) {
//        this.ignoreCase = ignoreCase;
//        if (ignoreCase) {
//            for (int i = 0; i < token.length; i ++) {
//                char ch = token[i];
//                if (ch >= 'A' && ch <= 'Z') {
//                    token[i] = (char)(ch - 'A' + 'a');
//                }
//            }
//        }
//    }

    public int getTokenLength() {
        return token.length;
    }

    private void init(char[] token) {
        int token_length = token.length;
        this.token = token;
        this.aRollback = new short[token_length];

		this.aRollback[0] = -1;
        int rollback_pos = 0;
        for (int idx = 1; idx < token_length; idx ++) {
            char curr_char = token[idx];
            if (curr_char == token[rollback_pos]) {
				// rollback-pos 위치의 문자와 같은 문자가 반복.
				// 해당 문자가 일치하지 않으면, rollback-pos의 문자와도 일치하지
				// 않으므로, 그 이전의 rollback-pos를 적용하고,
				// 다음 문자의 rollback-pos는 증가시킨다.
                aRollback[idx] = aRollback[rollback_pos];
            }
            else {
				// 현재 위치의 문자가 일치하지 않을때의 rollbac-pos 지정.
                aRollback[idx] = (short)rollback_pos;
				// 다음 문자의 rollback-pos을 이전 rollcak-string과 같은 위치가
				// 되도록 조정.
                while ((rollback_pos = aRollback[rollback_pos]) >= 0) {
                    if (curr_char == token[rollback_pos]) {
                        break;
                    }
                }
            }
            rollback_pos ++;
        }
    }


    public int next(int idx, int curr_char) {
        if (ignoreCase && curr_char >= 'A' && curr_char <= 'Z') {
            curr_char = (char)(curr_char - 'A' + 'a');
        }
        while (true) {
            int kode = token[idx];
            if (kode == curr_char) {
                break;
            }
            idx = aRollback[idx];
			if (idx < 0) {
				return 0;
			}
        }
        return (++idx >= token.length) ? -1 : idx;
    }

    public int scan(Reader reader) throws IOException {
        int idx = 0;
        int curr_char;
        for (int offset = 0; (curr_char = reader.read()) >= 0; offset ++) {
            idx = next(idx, curr_char);
            if (idx < 0) {
                return offset;
            }
        }
        return -1;
    }

//    public int scan(char[] text, int offset, int length) {
//        int idx = 0;
//        int end_offset = text.length - token.length;
//        for (; offset < end_offset; offset ++) {
//            idx = next(idx, text[offset]);
//            if (idx < 0) {
//                return offset;
//            }
//        }
//        return -1;
//    }
//
//    public int scan(String str, int offset, int length) {
//        int idx = 0;
//        int end_offset = str.length() - token.length;
//        for (; offset < end_offset; offset ++) {
//            idx = next(idx, str.charAt(offset));
//            if (idx < 0) {
//                return offset;
//            }
//        }
//        return -1;
//    }
//
//    public int scan(InputStream is) throws IOException {
//        int idx = 0;
//        int curr_char;
//        for (int offset = 0; (curr_char = is.read()) >= 0; offset ++) {
//            idx = next(idx, curr_char);
//            if (idx < 0) {
//                return offset;
//            }
//        }
//        return -1;
//    }
//
//    public int scan(byte[] data, int offset, int length) {
//        int idx = 0;
//        int end_offset = data.length - token.length;
//        for (; offset < end_offset; offset ++) {
//            idx = next(idx, data[offset]);
//            if (idx < 0) {
//                return offset;
//            }
//        }
//        return -1;
//    }

}
