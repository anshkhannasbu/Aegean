/*
 * Copyright 2004-2010 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.build.code;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * Switched source code to a specific Java version, automatically to the current
 * version, or enable / disable other blocks of source code in Java files.
 */
public class SwitchSource {

    private ArrayList<String> enable = new ArrayList<String>();
    private ArrayList<String> disable = new ArrayList<String>();

    /**
     * This method is called when executing this application from the command
     * line.
     *
     * @param args the command line parameters
     */
    public static void main(String... args) throws IOException {
        new SwitchSource().run(args);
    }

    private void run(String... args) throws IOException {
        String dir = null;
        String version = null;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("-dir".equals(a)) {
                dir = args[++i];
            } else if ("-auto".equals(a)) {
                enable.add("AWT");
                version = System.getProperty("java.specification.version");
            } else if ("-version".equals(a)) {
                version = args[++i];
            } else if (a.startsWith("-")) {
                disable.add(a.substring(1));
            } else if (a.startsWith("+")) {
                enable.add(a.substring(1));
            } else {
                showUsage();
                return;
            }
        }
        if (version == null) {
            // ok
        } else if ("1.3".equals(version)) {
            enable.add("Java 1.3 only");
            disable.add("Java 1.4");
            disable.add("Java 1.5");
            disable.add("Java 1.6");
        } else if ("1.4".equals(version)) {
            disable.add("Java 1.3 only");
            enable.add("Java 1.4");
            disable.add("Java 1.5");
            disable.add("Java 1.6");
        } else if ("1.5".equals(version)) {
            disable.add("Java 1.3 only");
            enable.add("Java 1.4");
            enable.add("Java 1.5");
            disable.add("Java 1.6");
        } else if (version.compareTo("1.6") >= 0) {
            disable.add("Java 1.3 only");
            enable.add("Java 1.4");
            enable.add("Java 1.5");
            enable.add("Java 1.6");
        } else {
            throw new IllegalArgumentException("version: " + version);
        }
        if (dir == null) {
            showUsage();
        } else {
            process(new File(dir));
        }
    }

    private void showUsage() {
        System.out.println("Switched source code to a specific Java version.");
        System.out.println("java " + getClass().getName() + "\n" +
                " -dir <dir>  The target directory\n" +
                " [-version]   Use the specified Java version (1.4 or newer)\n" +
                " [-auto]      Auto-detect Java version (1.4 or newer)\n" +
                " [+MODE]     Enable code labeled MODE\n" +
                " [-MODE]     Disable code labeled MODE");
    }

    private void process(File f) throws IOException {
        String name = f.getName();
        if (name.startsWith(".svn")) {
            return;
        } else if (name.endsWith(".java")) {
            processFile(f);
        } else if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                process(file);
            }
        }
    }

    private void processFile(File f) throws IOException {
        RandomAccessFile read = new RandomAccessFile(f, "r");
        byte[] buffer;
        try {
            long len = read.length();
            if (len >= Integer.MAX_VALUE) {
                throw new IOException("Files bigger than Integer.MAX_VALUE are not supported");
            }
            buffer = new byte[(int) len];
            read.readFully(buffer);
        } finally {
            read.close();
        }
        boolean found = false;
        // check for ## without creating a string
        for (int i = 0; i < buffer.length - 1; i++) {
            if (buffer[i] == '#' && buffer[i + 1] == '#') {
                found = true;
                break;
            }
        }
        if (!found) {
            return;
        }
        String source = new String(buffer);
        String target = source;
        target = replaceAll(target, "//##", "//##");
        for (String x : enable) {
            target = replaceAll(target, "/*## " + x + " begin ##", "//## " + x + " begin ##");
            target = replaceAll(target, "## " + x + " end ##*/", "//## " + x + " end ##");
        }
        for (String x : disable) {
            target = replaceAll(target, "//## " + x + " begin ##", "/*## " + x + " begin ##");
            target = replaceAll(target, "//## " + x + " end ##", "## " + x + " end ##*/");
        }
        if (!source.equals(target)) {
            String name = f.getPath();
            File fileNew = new File(name + ".new");
            FileWriter write = new FileWriter(fileNew);
            write.write(target);
            write.close();
            File fileBack = new File(name + ".bak");
            fileBack.delete();
            f.renameTo(fileBack);
            File fileCopy = new File(name);
            if (!fileNew.renameTo(fileCopy)) {
                throw new IOException("Could not rename " + fileNew.getAbsolutePath() + " to " + name);
            }
            if (!fileBack.delete()) {
                throw new IOException("Could not delete " + fileBack.getAbsolutePath());
            }
            // System.out.println(name);
        }
    }

    private static String replaceAll(String s, String before, String after) {
        int index = 0;
        while (true) {
            int next = s.indexOf(before, index);
            if (next < 0) {
                return s;
            }
            s = s.substring(0, next) + after + s.substring(next + before.length());
            index = next + after.length();
        }
    }

}
