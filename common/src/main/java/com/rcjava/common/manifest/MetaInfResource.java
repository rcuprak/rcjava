package com.rcjava.common.manifest;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Resource in the META-INF directory
 * @author Ryan Cuprak
 */
public class MetaInfResource {

    /**
     * JAR file, so we can extract the content if asked
     */
    private final File jarFile;

    /**
     * Name of the file
     */
    private final String name;

    /**
     * Path of the file within the jar file (includes the name of the file)
     */
    private final String fullyQualifiedName;

    /**
     * JAR Entry... this along with the path to the JAR file enables us to extract the content
     */
    private final JarEntry entry;

    /**
     * Finger print of the resource
     */
    private String fingerPrint;

    /**
     * Constructs a new MetaInfResource
     * @param name - name of the resource
     * @param fullyQualifiedName - path including the name
     * @param jarFile - jar file
     * @param entry - jar entry
     */
    public MetaInfResource(String name, String fullyQualifiedName,  File jarFile, JarEntry entry) {
        this.name = name;
        this.fullyQualifiedName = fullyQualifiedName;
        this.entry = entry;
        this.jarFile = jarFile;
    }

    /**
     * Name of the file
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Path to the file
     * @return Path including name
     */
    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    /**
     * Returns the fingerprint
     * @return fingerprint
     */
    public String getFingerPrint() {
        return fingerPrint;
    }

    /**
     * Retrieves the content of the file
     * @param charset - charset
     * @return content as a string
     * @throws IOException - thrown if there is an error
     */
    public String getContent(Charset charset) throws IOException {
        try(JarFile file = new JarFile(jarFile)) {
            try (InputStream is = file.getInputStream(entry);
                 DataInputStream dis = new DataInputStream(is)) {
                return new String(dis.readAllBytes(),charset);
            }
        }
    }

}
