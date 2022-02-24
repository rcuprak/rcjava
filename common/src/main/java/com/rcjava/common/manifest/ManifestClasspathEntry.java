package com.rcjava.common.manifest;

/**
 * Manifest classpath entry
 * @author Ryan cuprak
 */
public class ManifestClasspathEntry implements Comparable<ManifestClasspathEntry> {

    /**
     * Jar name
     */
    private final String jarName;

    /**
     * Full path to the JAR
     */
    private final String fullPath;

    /**
     * Creates a new manifest entry
     * @param value - value from the file
     */
    public ManifestClasspathEntry(String value) {
        this.jarName = value.substring(value.lastIndexOf("/") + 1);
        this.fullPath = value;
    }

    /**
     * Returns the jar name
     * @return jar name
     */
    public String getJarName() {
        return jarName;
    }

    /**
     * Returns the full path
     * @return path
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * Returns the jar name
     * @return jar name
     */
    public String toString() {
        return jarName;
    }

    /**
     * @param   o the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(ManifestClasspathEntry o) {
        return jarName.compareTo(o.getJarName());
    }
}
