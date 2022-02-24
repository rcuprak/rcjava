package com.rcjava.common.manifest;

import com.rcjava.common.JarUtil;
import com.rcjava.common.ManifestEditor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Test the manifest analyzer
 * @author Ryan Cuprak
 */
public class ManifestEditorTests {

    /**
     * Test JAR
     */
    private static final String TEST_JAR = "/com/rcjava/common/manifest/test.jar";

    /**
     * Non JAR to use in testing
     */
    private static final String NON_JAR = "/com/rcjava/common/manifest/notJar.txt";

    /**
     * Empty jar file
     */
    private static final String EMPTY_JAR = "/com/rcjava/common/manifest/empty.jar";

    /**
     * Empty manifest
     */
    private static final String EMPTY_MANIFEST = "/com/rcjava/common/manifest/emptyManifest.jar";

    /**
     * Jar file used in test
     */
    private Path jarFile;

    /**
     * Non-jar file
     */
    private static Path nonjarFile;

    /**
     * Empty JAR file
     */
    private static Path emptyJar;

    /**
     * Empty manifest
     */
    private static Path emptyManifest;

    /**
     * Init issue
     */
    private static IOException initIssue;

    /**
     * Executed before all of the tests
     * @throws IOException - thrown if there is a problem initializing
     */
    @BeforeAll
    public static void beforeAll() throws IOException {
        Function<String,Path> test = (String name) -> {
            try {
                Path file = Files.createTempFile("tst", "bin");
                try (InputStream is = ManifestEditor.class.getResourceAsStream(name)) {
                    if (is == null) {
                        throw new IOException("Unable to load non-JAR");
                    }
                    FileUtils.copyInputStreamToFile(is, file.toFile());
                }
                return file;
            } catch (IOException e) {
                initIssue = e;
            }
            return null;
        };
        // make sure we throw an exception if there is a problem...
        if(initIssue != null) {
            throw initIssue;
        }
        nonjarFile = test.apply(NON_JAR);
        emptyJar = test.apply(EMPTY_JAR);
        emptyManifest = test.apply(EMPTY_MANIFEST);
    }

    /**
     * Cleans-up after the tests
     */
    @AfterAll
    public static void cleanupAll() throws IOException {
       if(nonjarFile != null) {
           Files.delete(nonjarFile);
       }
        if(emptyJar != null) {
            Files.delete(emptyJar);
        }
        if(emptyManifest != null) {
            Files.delete(emptyManifest);
        }
    }

    /**
     * Creates tmp files with the JAR files to analyze
     * @throws IOException - thrown for errors
     */
    @BeforeEach
    public void setup() throws IOException {
        jarFile = Files.createTempFile("test","jar");
        try (InputStream is = ManifestEditor.class.getResourceAsStream(TEST_JAR)) {
            if(is == null) {
                throw new IOException("Unable to load test JAR");
            }
            FileUtils.copyInputStreamToFile(is,jarFile.toFile());
        }
    }

    /**
     * Delets the temp files
     */
    @AfterEach
    public void cleanup() throws IOException {
        if(jarFile != null) {
            Files.delete(jarFile);
        }
    }

    /**
     * Verifies the contents of a manifest can be dumped
     * @throws IOException - thrown for errors
     */
    @Test
    public void verifyDump() throws IOException {
        ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
        List<ManifestClasspathEntry> entries = ma.getClasspathEntries();
        Assertions.assertEquals(2,entries.size());
        Assertions.assertEquals("A.jar",entries.get(0).getJarName());
        Assertions.assertEquals("B.jar",entries.get(1).getJarName());
        Assertions.assertEquals(ma.getEntries().getValue("Created-By"),"Apache Maven Bundle Plugin");
        List<MetaInfResource> resources = ma.getResources();
        Assertions.assertEquals(4,resources.size());
        for(MetaInfResource resource : resources) {
            if(resource.getName().equals("LICENSE.md")) {
                Assertions.assertEquals("License",resource.getContent(StandardCharsets.ISO_8859_1));
            }
        }
    }

    /**
     * Tests that an exception is thrown if we attempt to open a file that is not a JAR file
     */
    @Test
    public void verifyNonJar() {
        IOException thrown = Assertions.assertThrows(IOException.class, () -> {
            ManifestEditor.createEditor(nonjarFile.toFile());
        });
        Assertions.assertEquals("zip END header not found", thrown.getMessage());
    }

    /**
     * Tests that the lack of a manifest is handled gracefully, this test should not throw an exception
     * @throws IOException - thrown for failures
     */
    @Test
    public void verifyNoManifest() throws IOException {
        ManifestEditor me = ManifestEditor.createEditor(emptyJar.toFile());
        Assertions.assertEquals(0,me.getClasspathEntries().size());

    }

    /**
     * Tests that logic can handle a MANIFEST with no classpath specified
     * @throws IOException - thrown if there is an exception
     */
    @Test
    public void verifyNoClasspath() throws IOException {
        ManifestEditor me = ManifestEditor.createEditor(emptyManifest.toFile());
        Assertions.assertEquals(0,me.getClasspathEntries().size());
    }

    /**
     * Tests regenerating the jar file
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void verifyRegenerate() throws IOException {
        Path jarFileCopy = Files.createTempFile("copy", "jar");
        try {
            ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
            ma.regenerate(jarFileCopy.toFile());
            Assertions.assertTrue(JarUtil.compare(jarFile.toFile(), jarFileCopy.toFile()), "Should have been the same jar!");
        } finally {
            Files.delete(jarFileCopy);
        }
    }

    /**
     * Tests adding to the classpath
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void verifyAddToClasspathSingle() throws IOException {
        Path jarFileCopy = Files.createTempFile("copy", "jar");
        try {
            ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
            ma.addJar("C.jar");
            ma.regenerate(jarFileCopy.toFile());
            Assertions.assertFalse(JarUtil.compare(jarFile.toFile(), jarFileCopy.toFile()), "Manifest should have been re-generated with a new class!");
            ManifestEditor reloaded = ManifestEditor.createEditor(jarFileCopy.toFile());
            boolean found = false;
            for(ManifestClasspathEntry mce : reloaded.getClasspathEntries()) {
                if(mce.getJarName().equals("C.jar")) {
                    found = true;
                    break;
                }
            }
            Assertions.assertTrue(found,"Classpath was not updated!");
        } finally {
            Files.delete(jarFileCopy);
        }
    }

    /**
     * Tests adding multiple entries to the classpath
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void verifyAddToClasspathMultiple() throws IOException {
        Path jarFileCopy = Files.createTempFile("copy", "jar");
        try {
            ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
            List<String> additions = new ArrayList<>(2);
            additions.add("C.jar");
            additions.add("D.jar");
            ma.addJars(additions);
            ma.regenerate(jarFileCopy.toFile());
            Assertions.assertFalse(JarUtil.compare(jarFile.toFile(), jarFileCopy.toFile()), "Manifest should have been re-generated with a new class!");
            ManifestEditor reloaded = ManifestEditor.createEditor(jarFileCopy.toFile());
            boolean found1 = false;
            boolean found2 = false;
            for(ManifestClasspathEntry mce : reloaded.getClasspathEntries()) {
                if(mce.getJarName().equals("C.jar")) {
                    found1 = true;
                }
                if(mce.getJarName().equals("D.jar")) {
                    found2 = true;
                }
            }
            Assertions.assertTrue(found1,"C.jar was not added.");
            Assertions.assertTrue(found2,"D.jar was not added.");
        } finally {
            Files.delete(jarFileCopy);
        }
    }

    /**
     * Verifies the classpath list
     */
    @Test
    public void verifyClasspathList() throws IOException {
        ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
        boolean foundA = false;
        boolean foundB = false;
        for(ManifestClasspathEntry me : ma.getClasspathEntries()) {
            if(me.getJarName().equals("A.jar")) {
                foundA = true;
            } else if (me.getJarName().equals("B.jar")) {
                foundB = true;
            }
        }
        Assertions.assertTrue(foundA,"Did not find A.jar.");
        Assertions.assertTrue(foundB,"Did not find B.jar.");
    }

    /**
     * Verifies that we can remove entries from the classpath
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void verifyRemoveSingle() throws Exception {
        Path jarFileCopy = Files.createTempFile("copy", "jar");
        try {
            ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
            ma.removeJar("A.jar");
            ma.regenerate(jarFileCopy.toFile());
            Assertions.assertFalse(JarUtil.compare(jarFile.toFile(), jarFileCopy.toFile()), "Same exact jar file should not have been generated!");
            ManifestEditor reloaded = ManifestEditor.createEditor(jarFileCopy.toFile());
            Assertions.assertEquals(1,reloaded.getClasspathEntries().size());
            Assertions.assertEquals(reloaded.getClasspathEntries().get(0).getJarName(),"B.jar");
        } finally {
            Files.delete(jarFileCopy);
        }
    }

    /**
     * Verifies that we can remove multiple jars
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void verifyRemoveMultiple() throws Exception {
        Path jarFileCopy = Files.createTempFile("copy", "jar");
        try {
            ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
            List<String> jars = new ArrayList<>();
            jars.add("A.jar");
            jars.add("B.jar");
            ma.removeJars(jars);
            ma.regenerate(jarFileCopy.toFile());
            Assertions.assertFalse(JarUtil.compare(jarFile.toFile(), jarFileCopy.toFile()), "Same exact jar file should not have been generated!");
            ManifestEditor reloaded = ManifestEditor.createEditor(jarFileCopy.toFile());
            Assertions.assertEquals(0,reloaded.getClasspathEntries().size());
        } finally {
            Files.delete(jarFileCopy);
        }
    }

    /**
     * Removes a non-existent entry existent
     */
    @Test
    public void removeNonexistent() throws IOException {
        ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
        List<String> jars = new ArrayList<>();
        jars.add("Z.jar");
        Exception thrown = Assertions.assertThrows(Exception.class, () -> {
            ma.removeJars(jars);
        });
        Assertions.assertTrue(thrown.getMessage().startsWith("Z.jar not present in classpath manifest."));
    }

    /**
     * Tests clearing a jar file
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void verifyClear() throws IOException {
        Path jarFileCopy = Files.createTempFile("copy", "jar");
        try {
            ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
            ma.clearClasspath();
            ma.regenerate(jarFileCopy.toFile());
            Assertions.assertFalse(JarUtil.compare(jarFile.toFile(), jarFileCopy.toFile()), "Same exact jar file should not have been generated!");
            ManifestEditor reloaded = ManifestEditor.createEditor(jarFileCopy.toFile());
            Assertions.assertEquals(0,reloaded.getClasspathEntries().size());
        } finally {
            Files.delete(jarFileCopy);
        }
    }

    /**
     * Verify that we can overwrite a jar file
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void verifyOverwrite() throws IOException {
        ManifestEditor ma = ManifestEditor.createEditor(jarFile.toFile());
        ma.clearClasspath();
        ma.regenerate(jarFile.toFile());
        ManifestEditor reloaded = ManifestEditor.createEditor(jarFile.toFile());
        Assertions.assertEquals(0,reloaded.getClasspathEntries().size());
    }

}
