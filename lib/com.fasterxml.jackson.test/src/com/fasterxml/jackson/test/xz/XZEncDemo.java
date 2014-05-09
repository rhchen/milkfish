package com.fasterxml.jackson.test.xz;
/*
 * XZEncDemo
 *
 * Author: Lasse Collin <lasse.collin@tukaani.org>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */

import java.io.*;

import org.tukaani.xz.*;

/**
 * Compresses a single file from standard input to standard ouput into
 * the .xz file format.
 * <p>
 * One optional argument is supported: LZMA2 preset level which is an integer
 * in the range [0, 9]. The default is 6.
 */
class XZEncDemo {
	
	private static final File fi = new File("data/android_systrace.txt");
	private static final File fo = new File("data/android_systrace.xz");
	
    public static void main(String[] args) throws Exception {
        LZMA2Options options = new LZMA2Options();

        options.setPreset(7);

        System.err.println("Encoder memory usage: "
                           + options.getEncoderMemoryUsage() + " KiB");
        System.err.println("Decoder memory usage: "
                           + options.getDecoderMemoryUsage() + " KiB");

        FileInputStream  fis = new FileInputStream(fi);
        FileOutputStream fio = new FileOutputStream(fo);
        
        XZOutputStream out = new XZOutputStream(fio, options);

        byte[] buf = new byte[8192];
        int size;
        while ((size = fis.read(buf)) != -1){
        	out.write(buf, 0, size);
        }
            

        out.finish();
        
        out.close();
        
        fis.close();
        
        System.out.println("Success write in "+ fo.getAbsolutePath());
    }
}
