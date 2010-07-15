package com.cerb4.impex;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class XMLThread extends Thread {
    Document doc = null;
    String fileName = "";

    public XMLThread(Document doc, String filename) {
        this.doc = doc;
        this.fileName = filename;
    }

    @Override
    public synchronized void start() {
        Boolean isVerbose = Boolean.valueOf(Configuration.get("verbose", "false"));
        String sExportEncoding = Configuration.get("exportEncoding", "ISO-8859-1");

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding(sExportEncoding);
        format.setOmitEncoding(false);

        OutputStream fileOutputStream = null;
        XMLWriter writer = null;
        try {
            fileOutputStream = new FileOutputStream(this.fileName);
            writer = new XMLWriter(new OutputStreamWriter(fileOutputStream, sExportEncoding), format);
            writer.write(doc);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            StreamUtils.closeSilently(fileOutputStream);
            doc.clearContent();
            doc = null;
        }

        if (isVerbose)
            System.out.println("Wrote " + this.fileName);

        this.interrupt();
    }

}
