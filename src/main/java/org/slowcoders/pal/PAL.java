package org.slowcoders.pal;

import org.slowcoders.io.util.NPAsyncScheduler;
import org.slowcoders.pal.io.Storage;
import org.slowcoders.util.ClassUtils;

public class PAL {

    private static final String CONFIG_IMPL = "org.slowcoders.pal.DefaultConfig";

    private static final Impl impl = ClassUtils.newInstance(CONFIG_IMPL);

    public static Storage getStorage() {
        return impl.getStorage();
    }

    public static NPAsyncScheduler.Executor getAsyncExecutor() {
        return impl.getAsyncExecutor();
    }

    public static boolean isDebugMode() {
        return impl.isDebugMode();
    }

    public static boolean isDebugVerbose() {
        return impl.isDebugVerbose();
    }

    public interface Impl {
        Storage getStorage();

        NPAsyncScheduler.Executor getAsyncExecutor();

        boolean isDebugVerbose();

        boolean isDebugMode();
    }
}
