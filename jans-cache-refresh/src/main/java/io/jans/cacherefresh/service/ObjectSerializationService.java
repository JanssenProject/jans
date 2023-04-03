/*
package io.jans.cacherefresh.service;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;

@ApplicationScoped
@Named
public class ObjectSerializationService {
    @Inject
    private Logger log;

    public ObjectSerializationService() {
    }

    public boolean saveObject(String path, Serializable obj, boolean append) {
        File file = new File(path);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, append);
        } catch (FileNotFoundException var12) {
            System.out.println("Faield to serialize to file: '{}'. Error: "+ path +" :  "+ var12);
            return false;
        }

        try {
            GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(fos));

            try {
                SerializationUtils.serialize(obj, gos);
                gos.flush();
            } catch (Throwable var10) {
                try {
                    gos.close();
                } catch (Throwable var9) {
                    var10.addSuppressed(var9);
                }

                throw var10;
            }

            gos.close();
            return true;
        } catch (IOException var11) {
            System.out.println("Faield to serialize to file: '{}'. Error: "+ path+ "  :  " + var11);
            return false;
        }
    }

    public boolean saveObject(String path, Serializable obj) {
        return this.saveObject(path, obj, false);
    }

    public Object loadObject(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("File '{}' is not exist"+  path);
            return null;
        } else {
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException var11) {
                System.out.println("Faield to deserialize from file: '{}'. Error: "+ path+ "  : " + var11);
                return null;
            }

            Object obj = null;

            try {
                GZIPInputStream gis = new GZIPInputStream(new BufferedInputStream(fis));

                try {
                    obj = SerializationUtils.deserialize(gis);
                } catch (Throwable var9) {
                    try {
                        gis.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }

                    throw var9;
                }

                gis.close();
                return obj;
            } catch (IOException var10) {
                System.out.println("Faield to deserialize from file: '{}'. Error: "+ path + "  : "+ var10);
                return null;
            }
        }
    }

    public void cleanup(String path) {
        File file = new File(path);
        FileUtils.deleteQuietly(file);
    }
}
*/
