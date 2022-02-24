package com.rcjava.common;

import com.rcjava.common.manifest.ManifestClasspathEntry;
import com.rcjava.common.products.Product;
import com.rcjava.common.products.Products;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.benf.cfr.reader.api.CfrDriver;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Summarizes a jar file. Performs operations on a jar file, such as removing a class.
 * @author Ryan Cuprak
 */
public class JarEditor implements Serializable, Cloneable {

    /**
     * Serial UID
     */
    static final long serialVersionUID = 1L;

    /**
     * JAR file to be analyzed
     */
    private final File jarFile;

    /**
     * Manifest editor, handles processing of the manifest
     */
    private ManifestEditor manifestEditor;

    /**
     * Products
     */
    private final Products products;

    /**
     * Product membership (entry would be JavaEE as the JAR contains Java EE jars)
     */
    private final Set<String> productMembership = new HashSet<>();

    /**
     * Classes keyed by fully qualified name (java.lang.String)
     */
    private final Map<String,ClassInfo> classes = new HashMap<>();

    /**
     * Resources
     */
    private final Map<String,String> resources = new HashMap<>();

    /**
     * Packages in the JAR file
     */
    private final Set<String> packages = new HashSet<>();

    /**
     * Fingerprint for comparison
     */
    private String fingerPrint;

    /**
     * Automatic module name, or null if there is none...
     */
    private String automaticModuleName;

    /**
     * Duplicate classes, in the event that the JAR contains duplicate classes which can happen!
     */
    private final Set<String> duplicates = new HashSet<>();

    /**
     * New additions to the JAR file, will appear when the jar file is regenerated
     */
    private final Map<String,File> additions = new HashMap<>();

    /**
     * Flag indicating the jar is signed
     */
    private boolean signed;

    /**
     * Version of the classes
     */
    private String version;

    /**
     * Flag indicating whether a jar file should be scanned
     */
    private final boolean scanJar;

    /**
     * All of the imports in the file
     */
    private Set<String> imports = new HashSet<>();

    /**
     * Creates a JAR editor but disables jar scanning
     */
    public JarEditor(File jarFile) {
        this(jarFile,false);
    }

    /**
     * Constructs a new JarInfo instance
     * @param jarFile - jar file to be analyzed
     */
    public JarEditor(File jarFile, boolean scanJar) {
        this.scanJar = scanJar;
        this.jarFile = jarFile;
        products = Products.getInstance();
    }

    /**
     * Returns true if the file is the same file
     * @return true if is the same file
     */
    public boolean isSameFile(JarEditor jarInfo) {
        return jarInfo.jarFile.getAbsolutePath().equals(jarFile.getAbsolutePath());
    }

    /**
     * Returns the fingerprint of the jar file
     * @return fingerprint
     */
    public String fingerPrint() {
        return fingerPrint;
    }

    /**
     * Returns true if the JAR file contains classes
     * @return classes
     */
    public boolean containsClasses() {
        return classes.size() > 0;
    }

    /**
     * Returns the manifest editor
     * @return manifest editor
     */
    public ManifestEditor getManifestEditor() {
        return manifestEditor;
    }

    /**
     * Returns the jar file
     * @return jar file
     */
    public File getJarFile() {
        return jarFile;
    }

    /**
     * Loads the data from the JAR file
     * @return this instance
     */
    public JarEditor load() throws IOException {
        manifestEditor = new ManifestEditor(this);
        try(JarFile jar = new JarFile(jarFile)) {
            manifestEditor.setManifest(jar.getManifest());
            Manifest mf = jar.getManifest();
            if(mf != null && mf.getMainAttributes() != null) {
                automaticModuleName = mf.getMainAttributes().getValue("Automatic-Module-Name");
                Enumeration<JarEntry> enumEntries = jar.entries();
                while (enumEntries.hasMoreElements()) {
                    JarEntry file = enumEntries.nextElement();
                    String fileName = file.getName().toUpperCase(Locale.ENGLISH);
                    if (fileName.endsWith(".SF") || fileName.endsWith(".DSA") || fileName.endsWith(".EC") || fileName.endsWith(".RSA")) {
                        signed = true;
                    } else if (!file.isDirectory()) {
                        if (file.getCodeSigners() != null && file.getCodeSigners().length > 0) {
                            signed = true;
                        }
                    }
                    manifestEditor.processEntry(file);
                    if (!file.isDirectory() && file.getName().toLowerCase().endsWith(".class")) {
                        try (InputStream is = jar.getInputStream(file)) {
                            String fullClassname = file.getName();
                            int index = fullClassname.lastIndexOf("/");
                            String pack, name;
                            if (index > 0) {
                                pack = fullClassname.substring(0, fullClassname.lastIndexOf("/"));
                                name = fullClassname.substring(fullClassname.lastIndexOf("/") + 1);
                            } else {
                                pack = "";
                                name = fullClassname;
                            }
                            if(classes.containsKey(fullClassname)) {
                                duplicates.add(fullClassname);
                            }
                            byte[] clazz = IOUtils.toByteArray(is);
                            Collection<String> clazzes = null;
                            if(scanJar) {
                                ClassPool cp = ClassPool.getDefault();

                                try {
                                    String trunc = fullClassname.substring(0,fullClassname.lastIndexOf("."));
                                    trunc = trunc.replaceAll("/",".");
                                    cp.insertClassPath(new ByteArrayClassPath(trunc, clazz));
                                    clazzes = cp.get(trunc).getRefClasses();
                                    imports.addAll(clazzes);
                                } catch (NotFoundException e) {
                                    throw new IOException(e);
                                }
                            }
                            String fingerprint = DigestUtils.md2Hex(clazz).toUpperCase();
                            classes.put(fullClassname, new ClassInfo(pack, name, fingerprint , file, clazzes));
                            packages.add(pack);
                            List<Product> prods = products.belongs(pack);
                            if(prods != null) {
                                for(Product product : prods) {
                                    productMembership.add(product.getDescription());
                                }
                            }
                        }
                        if(version == null) {
                            try (InputStream is = jar.getInputStream(file)) {
                                version = JarUtil.getVersion(is);
                            }
                        }
                    } else if(!file.isDirectory()) {
                        try (InputStream is = jar.getInputStream(file)) {
                            String checksum = DigestUtils.md2Hex(is).toUpperCase();
                            resources.put(file.getName(),checksum);
                        }
                    }
                }
            }
        }
        List<String> keys = new ArrayList<>(classes.size());
        keys.addAll(classes.keySet());
        keys.addAll(resources.values());
        Collections.sort(keys);
        StringBuilder builder = new StringBuilder();
        for(String key : keys) {
            builder.append(key);
        }
        fingerPrint = DigestUtils.md2Hex(builder.toString()).toUpperCase();
        return this;
    }

    /**
     * Returns product membership
     * @return product membership
     */
    public Set<String> getProductMembership() {
        return productMembership;
    }

    /**
     * Returns the class summary
     * @return class summary
     */
    public List<String[]> getClassSummary() {
        List<String[]> summary = new ArrayList<>(classes.size());
        for(Map.Entry<String,ClassInfo> entry : classes.entrySet()) {
            summary.add(new String[] {entry.getValue().getFQCN() , entry.getValue().getChecksum()});
        }
        summary.sort(Comparator.comparing(o -> o[0]));
        return summary;
    }

    /**
     * Returns true if they are equal
     * @param obj - object
     * @return true if equals
     */
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof JarEditor ji) {
            return fingerPrint.equals(ji.fingerPrint);
        }
        return false;
    }

    /**
     * Returns true if there is an overlap
     * @return true if overlaps
     */
    public Set<String> getOverlaps(JarEditor jarInfo) {
        Set<String> s = new HashSet<>(classes.keySet());
        // retains the ones that are overlapping
        s.retainAll(jarInfo.classes.keySet());
        if(!s.isEmpty()) {
            return s;
        }
        return null;
    }

    /**
     * Searches the jar for the fully qualified class name
     * @param fqcn - fully qualified class name
     * @return fully qualified class name
     */
    public boolean hasFullyQualifiedClass(String fqcn) {
        for(String qualifiedClass : classes.keySet()) {
            if(qualifiedClass.equals(fqcn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the resource is present
     * @param resource - resource
     * @return true if present
     */
    public boolean hasResource(String resource) {
        return resources.containsKey(resource);
    }

    /**
     * Returns the packages
     * @return packages
     */
    public Set<String> getPackages() {
        return packages;
    }

    /**
     * Returns the number of classes
     * @return class count
     */
    public int getClassCount() {
        return classes.size();
    }

    /**
     * Returns the automatic module name
     * @return automatic module name
     */
    public String getAutomaticModuleName() {
        return automaticModuleName;
    }

    /**
     * Returns the jar name
     * @return jar name
     */
    public String getJarName() {
        return jarFile.getName();
    }

    /**
     * Returns the name of the jar
     * @return jar name
     */
    @Override
    public String toString() {
        return getJarName();
    }

    /**
     * Returns the path to the file
     * @return path
     */
    public String getPath() {
        return jarFile.getParent();
    }

    /**
     * Returns true if the JAR file is signed
     * @return true if jar file signed
     */
    public boolean isSigned() {
        return signed;
    }

    /**
     * Returns true if the jar has duplicates (malformed Apache jar most likely!)
     * @return true if has duplicates
     */
    public boolean hasDuplicates() {
        return duplicates.size() > 0;
    }

    /**
     * Returns the duplicate classes
     * @return duplicates
     */
    public Set<String> getDuplicates() {
        return duplicates;
    }

    /**
     * Returns a hashcode so that we can use this in sets
     * @return hashcode
     */
    @Override
    public int hashCode() {
        if(fingerPrint == null) {
            throw new RuntimeException("Fingerprint not computed yet.");
        }
        return fingerPrint.hashCode();
    }

    /**
     * Returns the list of classes sorted...
     * @return classes
     */
    public List<String> getClasses() {
        List<String> strClasses = new ArrayList<>(classes.size());
        for(Map.Entry<String,ClassInfo> entry : classes.entrySet()) {
            strClasses.add(entry.getValue().getPackaging() + "/" + entry.getValue().getClassName());
        }
        Collections.sort(strClasses);
        return strClasses;
    }

    /**
     * Returns true if this JAR contains the class provided
     * @param fqcn - fully qualified clas name
     * @return true if present
     */
    public boolean hasClass(String fqcn) {
        return classes.containsKey(fqcn);
    }

    /**
     * Returns the list of non-java files in a JAR
     * @return resources
     */
    public List<String> getResources() {
        return new ArrayList<>(resources.keySet());
    }

    /**
     * Removes a class from the JAR file
     * @param fqcn - fully qualified class name
     */
    public void removeClass(String fqcn) throws IOException {
        if(signed) {
            throw new IOException("This JAR file is digitally signed.");
        }
        if(classes.remove(fqcn) == null) {
            throw new IOException(fqcn + " not found.");
        }
    }

    /**
     * Extracts and decompiles a java class
     * @param fqcn - fully qualified class name
     * @throws IOException - thrown if there is an error decompiling
     */
    public void decompileClass(String fqcn, File targetDirectory) throws IOException {
        if(!classes.containsKey(fqcn)) {
            throw new IOException(fqcn + " does not exist in the jar file.");
        }
        // Extract the class
        ClassInfo classInfo = classes.get(fqcn);
        JarEntry entry = classInfo.getJarEntry();
        Path p = Files.createTempFile("tmp","class");
        Files.delete(p);
        try (JarFile file = new JarFile(jarFile)) {
            Files.copy(file.getInputStream(entry),p);
            JarUtil.decompileClass(targetDirectory,p.toFile().getAbsolutePath());
        } finally {
            Files.delete(p);
        }
    }

    /**
     * Adds a file to a jar file. Note, if the file is a class, it won't appear in the class list
     * unless the file is reloaded.
     * @param path - path of the file
     * @param file - file to be added
     */
    public void addFile(String path, File file) throws IOException {
        if(signed) {
            throw new IOException("This JAR file is digitally signed.");
        }
        if (additions.containsKey(path+"/"+file.getName())) {
            throw new IOException(path + " already exists.");
        }
        additions.put(path,file);
    }

    /**
     * Regenerates the JAR file, basically extract
     */
    public void regenerate(File newJarFile) throws IOException {
        File tmpDir = Files.createTempDirectory("dup").toFile();
        // First we need to unpack the JAR file
        try(JarFile jar = new JarFile(jarFile)) {
            String destDir = tmpDir.getAbsolutePath();
            for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
                JarEntry entry = enums.nextElement();
                String fileName = destDir + File.separator + entry.getName();
                File f = new File(fileName);
                if (fileName.endsWith("/")) {
                    if (!f.mkdirs()) {
                        throw new IOException("Unable to create " + f.getAbsolutePath());
                    }
                }
            }
            for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
                JarEntry entry = enums.nextElement();
                String fileName = destDir + File.separator + entry.getName();
                File f = new File(fileName);
                if (!fileName.endsWith("/")) {
                    if(entry.getName().equals("META-INF/MANIFEST.MF")) {
                        manifestEditor.generateManifest(f);
                    } else {
                        if(!entry.getName().endsWith(".class") || classes.containsKey(entry.getName())) {
                            try (InputStream is = jar.getInputStream(entry);
                                 FileOutputStream fos = new FileOutputStream(f)) {
                                while (is.available() > 0) {
                                    fos.write(is.read());
                                }
                            }
                        }
                    }
                }
            }
        }
        // Let's add the new entries to the file
        for(Map.Entry<String,File> entry : additions.entrySet()) {
           File dir = new File(tmpDir.getAbsolutePath()+File.separator+entry.getKey());
           if(!dir.mkdirs()) {
               throw new IOException("Unable to create: " + dir.getAbsolutePath());
           }
           try(InputStream is = new FileInputStream(entry.getValue())) {
               Path target = Paths.get(dir.getAbsolutePath()+File.separator+entry.getValue().getName());
               Files.copy(is,target);
           }
        }
        // now we need to re-assemble it
        try(JarOutputStream jos = new JarOutputStream(new FileOutputStream(newJarFile))) {
            File[] files = tmpDir.listFiles();
            if(files != null) {
                for(File file : files) {
                    add(tmpDir.getPath(),file,jos);
                }
            }
        }
    }

    /**
     * Adds a file to the archive
     * @param base - base path
     * @param source - source file
     * @param target - target output stream
     * @throws IOException - thrown if there is an error
     */
    private static void add(String base, File source, JarOutputStream target) throws IOException {
        String name = source.getPath().substring(base.length()+1);
        name = name.replaceAll("\\\\","/");
        if (source.isDirectory()) {
            if (!name.endsWith("/")) {
                name += "/";
            }
            JarEntry entry = new JarEntry(name);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            target.closeEntry();
            File[] files = source.listFiles();
            if(files != null) {
                for (File nestedFile : files) {
                    add(base,nestedFile, target);
                }
            }
        } else {
            JarEntry entry = new JarEntry(name);
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source))) {
                byte[] buffer = new byte[1024];
                while (true) {
                    int count = in.read(buffer);
                    if (count == -1)
                        break;
                    target.write(buffer, 0, count);
                }
                target.closeEntry();
            }
        }
    }

    /**
     * Returns the version of the classes in the file
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Retrieves the imports that are used
     * @return imports
     */
    public Set<String> getImports() {
        return imports;
    }

    /**
     * Clones the object
     * @return cloned
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Object clone() {
        JarEditor jarInfo = new JarEditor(jarFile);
        jarInfo.resources.putAll(resources);
        jarInfo.packages.addAll(packages);
        jarInfo.fingerPrint = fingerPrint;
        jarInfo.automaticModuleName = automaticModuleName;
        jarInfo.duplicates.addAll(duplicates);
        for(Map.Entry<String,ClassInfo> entry : classes.entrySet()) {
            jarInfo.classes.put(entry.getKey(),(ClassInfo) entry.getValue().clone());
        }
        return jarInfo;
    }
}
