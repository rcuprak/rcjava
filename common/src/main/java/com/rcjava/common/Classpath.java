package com.rcjava.common;

import com.rcjava.common.manifest.ManifestClasspathEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents a classpath
 * @author Ryan Cuprak
 */
public class Classpath {

    /**
     * Jars comprising the classpath
     */
    private final List<JarEditor> jars = new LinkedList<>();

    /**
     * Directory where we start the search
     */
    private File base;

    /**
     * Default constructor
     */
    public Classpath(File base) {}

    /**
     * Constructs a new classpath with the specified jars
     * @param jars - jars
     */
    public Classpath(File base, List<JarEditor> jars) {
        this.jars.addAll(jars);
    }

    /**
     * Adds a jar to the classpath
     * @param jarEditor - jar to be added
     */
    public void addJar(JarEditor jarEditor) {
        jars.add(jarEditor);
    }

    /**
     * Returns the number of signed jars on the classpath
     * @return
     */
    public int getSignedJarCount() {
       throw new RuntimeException("Need to be implemented");
    }

    /**
     * Finds the jars containing a given class
     * @param fqcn - fully qualified class name
     * @return JarEditor
     */
    public List<JarEditor> findClass(String fqcn) {
        List<JarEditor> hits = new ArrayList<>(5);
        for(JarEditor jarEditor : jars) {
            if(jarEditor.hasClass(fqcn)) {
                hits.add(jarEditor);
            }
        }
        return hits;
    }

    /**
     * Returns all the classes on the classpath
     * @return classes
     */
    public Set<String> getAllClasses() {
        Set<String> uniqueClassSet = new HashSet<>();
        for(JarEditor je : jars) {
            uniqueClassSet.addAll(je.getClasses());
        }
        return uniqueClassSet;
    }

    /**
     * Returns the unique set of versions
     * @return set of unique versions
     */
    public Set<String> getVersions() {
        Set<String> versions = new HashSet<>();
        for(JarEditor je : jars) {
            versions.add(je.getVersion());
        }
        return versions;
    }

    /**
     * Gets the list of JARs that comprise the classpath
     * @return jars
     */
    public List<JarEditor> getJars() {
        return jars;
    }

    /**
     * Analyzes the classpath, for duplicates
     */
    public void analyze() {
        for(JarEditor jar : jars) {
            List<ManifestClasspathEntry> classpathEntries = jar.getManifestEditor().getClasspathEntries();
            for(ManifestClasspathEntry mce : classpathEntries) {
                mce.getFullPath();
            }
        }
    }



}
