package org.slowcoders.pal.io;

public interface Clipboard {

    void setContent(String plainText, String htmlText);

    String getTextContent();

    String getHtmlContent();
}
