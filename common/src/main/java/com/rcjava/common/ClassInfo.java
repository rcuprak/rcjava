package com.rcjava.common;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.jar.JarEntry;

/**
 * ClassInfo - encapsulates the summary of a class
 * @author Ryan Cuprak
 */
public class ClassInfo {

    /**
     * Package in which the class resides
     */
    private final String packaging;

    /**
     * Name of the class, has '.class' on the end
     */
    private final String className;

    /**
     * Checksum of the class
     */
    private final String checksum;

    /**
     * Jar Entry
     */
    private final JarEntry jarEntry;

    /**
     * Imports in the class
     */
    private final Collection<String> myImports;

    /**
     * Constructs a new ClassInfo object
     * @param packaging - packaging
     * @param className - classname
     * @param checksum - checksum
     */
    public ClassInfo(String packaging, String className, String checksum, JarEntry jarEntry, Collection<String> myImports) {
        this.packaging = packaging;
        this.className = className;
        this.checksum = checksum;
        this.jarEntry = jarEntry;
        this.myImports = myImports;
    }

    /**
     * Returns the packaging
     * @return packaging
     */
    public String getPackaging() {
        return packaging;
    }

    /**
     * Returns the classname
     * @return classname
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the checksum
     * @return checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Returns the JarEntry
     * @return jar entry
     */
    public JarEntry getJarEntry() {
        return jarEntry;
    }

    /**
     * Returns the fully qualified class name
     * @return fully qualified class name
     */
    public String getFQCN() {
        return packaging + "/" + className;
    }

    /**
     * Returns the imports
     * @return imports
     */
    public Collection<String> getImports() {
        return myImports;
    }

    /**
     * Clones this class
     * @return ClassInfo
     */
    @Override
    public ClassInfo clone() {
        return new ClassInfo(packaging,className,checksum,jarEntry,myImports);
    }
}
