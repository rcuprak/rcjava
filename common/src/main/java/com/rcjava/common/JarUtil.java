package com.rcjava.common;

import com.rcjava.common.compile.CustomSinkFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.benf.cfr.reader.api.CfrDriver;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

public class JarUtil {

    /**
     * File extensions for signed jars
     */
    private static final Set<String> ENDINGS = new HashSet<>();

    /**
     * Buffer used in generating the file
     */
    private static final byte[] buffer = new byte[4096];

    static {
        ENDINGS.add(".SF");
        ENDINGS.add(".DSA");
        ENDINGS.add(".EC");
        ENDINGS.add(".RSA");
    }

    /**
     * Compares two JAR files
     * @param jar1 - first jar
     * @param jar2 - second jar
     */
    public static boolean compare(File jar1, File jar2) throws IOException {
        if(jar1.getAbsolutePath().equals(jar2.getAbsolutePath())) {
            throw new IOException("Comparing same exact file: " + jar1.getAbsolutePath() + " to " + jar2.getAbsolutePath());
        }
        Map<String,String> checksumJar1 = generateChecksums(jar1);
        Map<String,String> checksumJar2 = generateChecksums(jar2);
        if(checksumJar1.size() != checksumJar2.size()) {
            return false;
        }
        for(Map.Entry<String,String> entry : checksumJar1.entrySet()) {
            if(!entry.getValue().equals(checksumJar2.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generates checksums for all of the files
     * @param jarFile - jar file
     * @return Map of entries to checksum
     * @throws IOException - thrown if there is an error
     */
    private static Map<String,String> generateChecksums(File jarFile) throws IOException {
        Map<String,String> entries = new HashMap<>();
        try(JarFile jar = new JarFile(jarFile)) {
            for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
                JarEntry entry = enums.nextElement();
                if (!entry.getName().endsWith("/")) {
                    if (entries.containsKey(entry.getName())) {
                        throw new IOException("JAR file contains a duplicate: " + entry.getName());
                    }
                    entries.put(entry.getName(), DigestUtils.md2Hex(jar.getInputStream(entry)).toUpperCase());
                }
            }
        }
        return entries;
    }

    /**
     * Extracts a class using the fully qualified class name
     * @param fqcn - fully qualified classname
     * @param dest - destination
     */
    public static void extractClass(String fqcn, File dest, String jarFile) throws IOException {
        String pck = JarUtil.extractPackage(fqcn);
        File dir;
        if(pck.length() > 0) {
            dir = new File(dest.getAbsolutePath() + File.separator + pck);
        } else {
            dir = new File(dest.getAbsolutePath());
        }
        if(!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Unable to create: " + dir.getAbsolutePath());
            }
        }
        try(JarFile file = new JarFile(jarFile)) {
            JarEntry je = file.getJarEntry(fqcn + ".class");
            if(je == null) {
                throw new IOException("Unable to find " + fqcn);
            }
            try(InputStream is = file.getInputStream(je)) {
                String name = je.getName();
                String classname = name.substring(name.lastIndexOf("/")+1);
                Path path = Paths.get(dir.getAbsolutePath() + File.separator + classname );
                Files.copy(is, path);
            }
        }
    }

    /**
     * Decompiles a class, puts it in the same directory
     * @param dest - destination where source files are to be placed
     * @param pathToClazz - the class we want to decompile
     */
    public static void decompileClass(File dest , String pathToClazz) {
        CustomSinkFactory csf = new CustomSinkFactory(dest);
        CfrDriver driver = new CfrDriver.Builder().withOutputSink(csf).build();
        List<String> clazzes = new LinkedList<>();
        clazzes.add(pathToClazz);
        driver.analyse(clazzes);
    }

    /**
     * Checks if a JAR file is signed. Assumes a jar file is signed if there are files in the META-INF directory
     * with .SF, .DSA, .EC< or .RSA. Also loops through the classes for completeness.
     * @param jarFile - jar file to check
     * @return boolean - true if the jar is signed
     * @throws IOException - thrown if there is a problem processing the JAR file
     */
    public static boolean checkSigned(File jarFile) throws IOException {
        boolean verify = true;
        try(JarFile jar = new JarFile(jarFile, verify)) {
            Enumeration<JarEntry> entries = jar.entries();
            while(entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String fileName = entry.getName().toUpperCase(Locale.ENGLISH);
                if (fileName.endsWith(".SF") || fileName.endsWith(".DSA") || fileName.endsWith(".EC") || fileName.endsWith(".RSA")) {
                    return true;
                } else if (!entry.isDirectory()) {
                    if (entry.getCodeSigners() != null && entry.getCodeSigners().length > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Duplicates a JAR file without
     * @param jarFile - JAR File
     * @param overwrite - true if we need to overwrite the jar file
     * @return path to unsigned jar
     */
    public static Path unsignJar(Path jarFile, boolean overwrite) throws IOException {
        Path unsignedPath = Files.createTempFile("tmp","jar");
        Path target;
        try(JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(unsignedPath.toFile()));
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile.toFile()))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                if (entry.getName().startsWith("META-INF")) {
                    boolean copy = true;
                    for (String ending : ENDINGS) {
                        if (entry.getName().endsWith(ending)) {
                            copy = false;
                            break;
                        }
                    }
                    if(copy) {
                        copyFile(outputStream,jarInputStream,entry);
                    }
                } else {
                    copyFile(outputStream,jarInputStream,entry);
                }
            }
        } finally {
            if(overwrite) {
                target = jarFile;
                Files.delete(jarFile);
                Files.copy(unsignedPath,jarFile);
                Files.delete(unsignedPath);
            } else {
                System.out.println("Jar: " + jarFile);
                String newName = jarFile.getName(jarFile.getNameCount()-1).toString();
                newName = newName.substring(0, newName.lastIndexOf('.')) + "_unsigned.jar";
                target = jarFile.getParent().resolve(newName);
                Files.move(unsignedPath,target);
            }
        }
        return target;
    }

    /**
     * Writes a stream
     */
    private static void copyFile(JarOutputStream outputStream,JarInputStream jarInputStream, JarEntry entry) throws IOException {
        outputStream.putNextEntry(new JarEntry(entry.getName()));
        int read;
        while((read = jarInputStream.read(buffer)) != -1) {
            outputStream.write(buffer,0,read);
        }
        outputStream.closeEntry();
    }

    /**
     * Extracts the package from a classname
     * @param fqcn - fully qualified class name
     */
    public static String extractPackage(String fqcn) {
        if(fqcn.contains("/")) {
            return fqcn.substring(0, fqcn.lastIndexOf("/"));
        }
        return "";
    }

    /**
     * Retrieves the version of a class
     * @return version
     */
    public static String getVersion(InputStream is) throws IOException {
        try(DataInputStream data = new DataInputStream(is)) {
            int magic = data.readInt();
            if (magic != 0xCAFEBABE) {
                throw new IOException("Invalid Java class");
            }
            int minor = 0xFFFF & data.readShort();
            int major = 0xFFFF & data.readShort();
            return major + "." + minor;
        }
    }

    /**
     *
     */
    public static void scanJar() {

    }
}
