package org.slowcoders.util;

import org.slowcoders.pal.PAL;

import java.sql.SQLException;

public class Debug {
    public static final boolean DEBUG = PAL.isDebugMode();
    public static final boolean DEBUG_VERBOSE = DEBUG & PAL.isDebugVerbose();
    public static boolean CAN_IGNORE_NOT_IMPLEMENTED = true;
    static volatile boolean pass = true;

    public static RuntimeException throwError(String message) {
        boolean pass = false;
        if (!pass) {
            throw new RuntimeException(message);
        }
        return null;
    }

    public static RuntimeException notImplemented() {
        return throwError("not implemented");
    }

    public static RuntimeException notImplemented(String msg) {
        return throwError("not implemented reason : " + msg);
    }

    public static void mustImplement(String msg) {
        String caller = new Throwable().getStackTrace()[1].getMethodName();
        System.err.print(caller + "() - " + msg);
    }

    public static void notTested(String msg) {
        throwError(msg);
    }

    public static void Assert(boolean b) {
        Assert(b, "Something wrong");
    }

    public static void Assert(boolean b, String msg) {
        if (!b) {
            throwError(msg);
        }
    }

    public static RuntimeException shouldNotBeHere(String msg) {
        return throwError(msg);
    }

    public static RuntimeException shouldNotBeHere() {
        return shouldNotBeHere("Should not be here!");
    }

    public static RuntimeException fatal(Throwable e) {
        return wtf(e);
    }

    public static RuntimeException fatal(String message) {
        return wtf(message);
    }

    public static RuntimeException wtf(Throwable e) {
        e.printStackTrace();
        throwError(e.getMessage());
        System.exit(-1);
        return null;
    }

    public static RuntimeException wtf(Throwable e, String msg) {
        e.printStackTrace();
        return throwError(msg);
    }

    public static RuntimeException wtf(String msg) {
        return throwError(msg);
    }

    public static void ignoreException(Exception e) {
        if (DEBUG_VERBOSE) {
            e.printStackTrace();
        }
    }

    public static void trap() {
        if (!pass) {
            System.err.print("");
        }
    }

    public static void debugWarning(String s) {
        RuntimeException e = new RuntimeException(s);
        e.fillInStackTrace();
        e.printStackTrace();
    }

    public static void warning(SQLException e) {
        wtf(e);
    }

    public static void verbose(String message) {
        if (DEBUG_VERBOSE) {
            System.out.println(message);
        }
    }

    public static boolean ignoreNotImplemented(String message) {
        if (CAN_IGNORE_NOT_IMPLEMENTED) {
            System.out.println(message);
            return true;
        }

        throw Debug.notImplemented();
    }

    public static boolean ignoreNotImplemented() {
        if (CAN_IGNORE_NOT_IMPLEMENTED) {
            RuntimeException e = new RuntimeException("Need implemented ");
            StackTraceElement tr = e.getStackTrace()[1];
            System.out.println("Not implemented!!\n\tat " + tr.getClassName() + "." + tr.getMethodName()
                    + "(" + tr.getFileName() + ":" + tr.getLineNumber() + ")\n\t...");
            return true;
        }

        throw Debug.notImplemented();
    }
}
