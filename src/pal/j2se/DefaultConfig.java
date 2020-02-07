package org.slowcoders.pal;

import org.slowcoders.io.util.NPAsyncScheduler;
import org.slowcoders.pal.io.Storage;
import org.slowcoders.util.Debug;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DefaultConfig implements PAL.Impl {

    private static final Storage storage = new Storage();
    private static final AsyncExecutor executor = new AsyncExecutor();

    @Override
    public Storage getStorage() {
        return storage;
    }

    @Override
    public NPAsyncScheduler.Executor getAsyncExecutor() {
        return executor;
    }

    @Override
    public boolean isDebugVerbose() {
        return false;
    }

    @Override
    public boolean isDebugMode() {
        return false;
    }

    private static class AsyncExecutor implements NPAsyncScheduler.Executor, Runnable{

        @Override
        public void run() {
            try {
                NPAsyncScheduler.executePendingTasks();
            }
            catch (Exception e) {
                Debug.wtf(e);
            }
        }
        @Override
        public void triggerAsync() {
            EventQueue.invokeLater(this);
        }

        @Override
        public boolean isInMainThread() {
            return EventQueue.isDispatchThread();
        }
    }

    private static class Storage implements org.slowcoders.pal.io.Storage {

        String dbDir = System.getProperty("user.home") + "/storm-dir";
        File settingsDir = new File(dbDir);
        File downloadDir = new File(dbDir + "/Downloads");
        File cacheDir = new File(dbDir + "/Cache");

        private DefaultStorage() {
            settingsDir.mkdirs();
            downloadDir.mkdirs();
            cacheDir.mkdirs();
        }

        public InputStream openInputStream(URI contentUri) throws IOException {
            File file;
            if (contentUri.getScheme() == null || !contentUri.getScheme().startsWith("file")) {
                //throw NPDebug.wtf("Only support file schema");
                file = new File(contentUri.toString());
            }
            else {
                file = new File(contentUri);
            }

            return new FileInputStream(file);
        }

        @Override
        public File getPreferenceDirectory() {
            return settingsDir;
        }

        @Override
        public String getDatabaseDirectory() {
            return dbDir;
        }

        @Override
        public File getDownloadDirectory() {
            return downloadDir;
        }

        @Override
        public File getCacheDirectory() {
            return cacheDir;
        }
    }
}
