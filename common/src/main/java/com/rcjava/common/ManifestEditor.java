package com.rcjava.common;

import com.rcjava.common.manifest.ManifestClasspathEntry;
import com.rcjava.common.manifest.MetaInfResource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Edits a manifest classpath
 * @author Ryan Cuprak
 */
public class ManifestEditor {

    /**
     * Classpath entries
     */
    private final List<ManifestClasspathEntry> classpathEntries = new LinkedList<>();

    /**
     * Files in the META-INF directory...
     */
    private final List<MetaInfResource> resources = new LinkedList<>();

    /**
     * Main attributes
     */
    private Attributes mainAttributes;

    /**
     * Manifest
     */
    private Manifest manifest;

    private JarEditor jarEditor;

    /**
     *
     * @param jarEditor
     */
    protected ManifestEditor(JarEditor jarEditor) {
        this.jarEditor = jarEditor;
    }

    /**
     * Creates a new manifest editor
     * @param file - jar file to edit
     * @return ManifestEditor instance
     */
    public static ManifestEditor createEditor(File file) throws IOException {
        JarEditor jarEditor = new JarEditor(file).load();
        return jarEditor.getManifestEditor();
    }

    /**
     * Processes an entry in the JAR file
     * Called from JarEditor.load
     */
    void processEntry(JarEntry jarEntry) {
        if(jarEntry.getRealName().startsWith("META-INF")) {
            String path = jarEntry.getRealName();
            if(!path.endsWith("/")) {
                String name = jarEntry.getRealName().substring(path.lastIndexOf("/")+1, path.length());
                resources.add(new MetaInfResource(name,path,jarEditor.getJarFile(),jarEntry));
            }
        }
    }

    /**
     * Processes the manifest. Called from the JarEditor.load
     * @param manifest - manifest
     */
    void setManifest(Manifest manifest) {
        this.manifest = manifest;
        if(manifest != null) {
            mainAttributes = manifest.getMainAttributes();
            String classpath = manifest.getMainAttributes().getValue("Class-Path");
            if(classpath != null) {
                StringTokenizer tokenizer = new StringTokenizer(classpath, " ");
                String element;
                while (tokenizer.hasMoreElements()) {
                    element = tokenizer.nextElement().toString();
                    classpathEntries.add(new ManifestClasspathEntry(element));
                }
            }
        }
    }

    /**
     * Returns the MetaInfo resources
     * @return resources in the meta-inf directory
     */
    public List<MetaInfResource> getResources() {
        return List.of(resources.toArray(new MetaInfResource[]{}));
    }

    /**
     * Removes all classpath
     */
    public void clearClasspath() {
        classpathEntries.clear();
    }

    /**
     * Returns all of the entries in the manifest
     * @return all entries
     */
    public Attributes getEntries() {
        return mainAttributes;
    }

    /**
     * Returns the classpath entries
     * @return classpath entries
     */
    public List<ManifestClasspathEntry> getClasspathEntries() {
        return classpathEntries;
    }

    /**
     * Removes a jar from a jar file
     * @param jar - jar to be removed
     */
    public void removeJar(String jar) throws Exception {
        Iterator<ManifestClasspathEntry> itr = classpathEntries.iterator();
        boolean found = false;
        while(itr.hasNext()) {
            ManifestClasspathEntry mce = itr.next();
            if(mce.getJarName().equals(jar)) {
                found = true;
                itr.remove();
                break;
            }
        }
        if(!found) {
            throw new Exception(jar+" not present in classpath manifest.");
        }
    }

    /**
     * Removes multiple jar files
     * @param jars jars
     */
    public void removeJars(List<String> jars) throws Exception {
        for(String jar : jars) {
            removeJar(jar);
        }
    }

    /**
     * Adds multiple items to the classpath
     * @param entries - entries
     */
    public void addJars(List<String> entries) {
        for(String entry : entries) {
            classpathEntries.add(new ManifestClasspathEntry(entry));
        }
    }

    /**
     * Adds an entry to the classpath
     * @param jar - jar to be added
     */
    public void addJar(String jar) {
        classpathEntries.add(new ManifestClasspathEntry(jar));
    }

    /**
     * Generates the manifest file
     * @param manifestFile - manifest file
     */
    void generateManifest(File manifestFile) throws IOException {
        StringBuilder builder = new StringBuilder();
        boolean addSpace = false;
        for(ManifestClasspathEntry cpe : classpathEntries) {
            if(addSpace) {
                builder.append(" ");
            } else {
                addSpace = true;
            }
            builder.append(cpe.getFullPath());
        }
        manifest.getMainAttributes().putValue("Class-Path",builder.toString());
        if(!manifestFile.createNewFile()) {
            throw new IOException("Unable to create: " + manifestFile.getAbsolutePath());
        }
        try (FileOutputStream fos = new FileOutputStream(manifestFile)) {
            manifest.write(fos);
        }
    }

    /**
     * Regenerates the JAR file and manifest. Delegates to the JarEditor
     * @param target - target
     */
    public void regenerate(File target) throws IOException {
        jarEditor.regenerate(target);
    }
}
