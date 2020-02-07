package org.slowcoders.pal.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface Storage {

    InputStream openInputStream(URI contentUri) throws IOException;

    File getPreferenceDirectory();

    String getDatabaseDirectory();

    File getDownloadDirectory();

    File getCacheDirectory();

}
