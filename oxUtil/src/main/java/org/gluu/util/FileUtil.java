/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtil {

    private FileUtil() { }

    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Writes data in a file on specified position
     *
     * @param filePath
     * @param position
     * @param data
     * @return
     */
    public boolean writeToFile(String filePath, long position, String data) {
        try {
            File f = new File(filePath);
            RandomAccessFile raf;
            raf = new RandomAccessFile(f, "rw");
            raf.seek(position);
            StringBuilder dataAfterPostion = new StringBuilder(data);
            while (raf.getFilePointer() < raf.length()) {
                String line = raf.readLine();
                dataAfterPostion.append(line);
            }
            raf.seek(position);
            raf.writeUTF(dataAfterPostion.toString());
            raf.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Write in a file from the beginning of file. As UTF write operation is for
     * 65535 bytes only. We need to write in chunks
     *
     * @param filePath
     * @param position
     * @param data
     * @return
     */
    public boolean writeLargeFile(String filePath, long position, String data) {
        try {
            // Create file
            FileWriter fstream = new FileWriter(filePath);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(data);
            // Close the output stream
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * returns the first occurrence of specified string in a file.
     *
     * @param filePath
     * @param searchStr
     * @return
     */
    public long findFirstPosition(String filePath, String searchStr) {
        try {
            File f = new File(filePath);
            RandomAccessFile raf;
            raf = new RandomAccessFile(f, "r");
            long position = -1;
            while (raf.getFilePointer() < raf.length()) {
                String line = raf.readLine();
                if (line.contains(searchStr)) {
                    position = raf.getFilePointer() + line.indexOf(searchStr) - (line.length() + 1);
                    break;
                }
            }
            return position;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public long findLastPosition(String filePath, String searchStr) {
        try {
            File f = new File(filePath);
            RandomAccessFile raf;
            raf = new RandomAccessFile(f, "r");
            long position = -1;
            while (raf.getFilePointer() < raf.length()) {
                String line = raf.readLine();
                if (line.contains(searchStr)) {
                    position = raf.getFilePointer() + line.indexOf(searchStr) - line.length();
                    continue;
                }
            }
            return position;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Read data from a URL
     *
     * @param url
     * @return
     */
    public String readFromURL(String url) {
        try {
            URL u = new URL(url);
            StringBuilder data = new StringBuilder();
            InputStream is = u.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            try {
                String theLine;
                while ((theLine = br.readLine()) != null) {
                    data.append(theLine);
                }
            } finally {
                IOUtils.closeQuietly(br);
            }
            return data.toString();
        } catch (MalformedURLException ex) {
            LOG.error(ex.getMessage(), ex);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return "";
    }

    /**
     * Read data from a file
     *
     * @param filePath
     * @return
     */
    public static String readFromFile(String filePath) {
        try {
            final StringBuilder data = new StringBuilder();
            FileInputStream fstream = new FileInputStream(filePath);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            try {
                String strLine = "";
                // Read File Line By Line
                while ((strLine = br.readLine()) != null) {
                    // Print the content on the console
                    data.append(strLine);
                }
            } finally {
                IOUtils.closeQuietly(br);
            }
            return data.toString();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }

    }

    public static StringBuffer readFile(String filePath) {
        StringBuffer buf = new StringBuffer();
        FileInputStream input = null;
        try {
            input = new FileInputStream(new File(filePath));
            byte[] buffer = new byte[10];
            while (true) {
                int i = input.read(buffer);
                if (i < 0) {
                    break;
                }
                byte[] outBuffer = new byte[i];
                System.arraycopy(buffer, 0, outBuffer, 0, i);
                buf.append(new String(outBuffer, "UTF-8"));
            }
        } catch (Exception ex) {
            LOG.debug("File read error: ", ex);
        } finally {
            IOUtils.closeQuietly(input);
        }
        return buf;
    }
}
