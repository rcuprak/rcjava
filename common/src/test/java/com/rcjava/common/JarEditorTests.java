package com.rcjava.common;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Tests extracting information from a jar file
 * @author Ryan Cuprak
 */
public class JarEditorTests {

    /**
     * Scratch jar
     */
    private static Path scratchJar1;

    /**
     * Scratch jar #2 (with an additional class)
     */
    private static Path scratchJar2;

    /**
     * This jar file contains a duplicate class
     */
    private static Path malformedJar;

    /**
     * Class we are going to add to an existing jar
     */
    private static Path clazz;

    /**
     * Signed jar
     */
    private static Path signedJar;

    /**
     * Executed before the tests
     */
    @BeforeAll
    public static void beforeTests() throws Exception {
        scratchJar1 = TestUtils.extract("/com/rcjava/common/jar/ClasspathScratch.jar");
        scratchJar2 = TestUtils.extract("/com/rcjava/common/jar/ClasspathScratch2.jar");
        malformedJar = TestUtils.extract("/com/rcjava/common/jar/MalformedJar.jar");
        signedJar = TestUtils.extract("/com/rcjava/common/jarutil/SignedJar.jar");
        clazz = TestUtils.extract("/com/rcjava/common/jar/Test2.bin",".class");
    }

    /**
     * Executed after the tests
     */
    @AfterAll
    public static void afterTests() throws IOException {
        if(scratchJar1 != null) {
            Files.delete(scratchJar1);
        }
        if(scratchJar2 != null) {
            Files.delete(scratchJar2);
        }
        if(malformedJar != null) {
            Files.delete(malformedJar);
        }
        if(signedJar != null) {
            Files.delete(signedJar);
        }
    }

    /**
     * Verifies class count works.
     */
    @Test
    public void testClassCount() throws Exception {
        JarEditor jarInfo = new JarEditor(scratchJar1.toFile()).load();
        Assertions.assertEquals(1,jarInfo.getClassCount());
        Set<String> packs = jarInfo.getPackages();
        Assertions.assertEquals(1,packs.size());
        Assertions.assertTrue(packs.contains("com/rcjava/scratch"));
    }

    /**
     * Test if the equals method works
     * Equals takes into account
     */
    @Test
    public void testEquals() throws IOException {
        JarEditor jarInfo1 = new JarEditor(scratchJar1.toFile()).load();
        JarEditor jarInfo2 = new JarEditor(scratchJar1.toFile()).load();
        Assertions.assertEquals(jarInfo1,jarInfo2);
        JarEditor different = new JarEditor(scratchJar2.toFile()).load();
        Assertions.assertNotEquals(jarInfo1,different);
    }

    /**
     * Verifies that overlapping jars can be detected.
     */
    @Test
    public void testOverlap() throws IOException {
        JarEditor jarInfo1 = new JarEditor(scratchJar1.toFile()).load();
        JarEditor jarInfo2 = new JarEditor(scratchJar2.toFile()).load();
        Set<String> overlaps = jarInfo1.getOverlaps(jarInfo2);
        Assertions.assertEquals(1,overlaps.size());
        Assertions.assertTrue(overlaps.contains("com/rcjava/scratch/Test.class"));
    }

    /**
     * Tests that we can find a class
     */
    @Test
    public void testFindClass() throws IOException {
        JarEditor jarInfo1 = new JarEditor(scratchJar1.toFile()).load();
        Assertions.assertTrue(jarInfo1.hasFullyQualifiedClass("com/rcjava/scratch/Test.class"));
        Assertions.assertFalse(jarInfo1.hasFullyQualifiedClass("com/rcjava/scratch/Nope.class"));
    }

    /**
     * Tests that non-Java files can be found
     */
    @Test
    public void testFindResource() throws IOException {
        JarEditor jarInfo1 = new JarEditor(scratchJar1.toFile()).load();
        Assertions.assertTrue(jarInfo1.hasResource("META-INF/MANIFEST.MF"));
    }

    /**
     * Tests that a jar file can be regenerated exactly as it is...
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void testRegenerateJAR() throws IOException {
        JarEditor jarEditor = new JarEditor(scratchJar1.toFile()).load();
        Path p = Files.createTempFile("test","jar");
        jarEditor.regenerate(p.toFile());
        JarUtil.compare(scratchJar1.toFile(),p.toFile());
    }

    /**
     * Tests the logic that lists classes
     */
    @Test
    public void testListClasses() throws IOException {
        JarEditor jarEditor = new JarEditor(scratchJar1.toFile()).load();
        List<String> classes = jarEditor.getClasses();
        Assertions.assertEquals(1,classes.size());
        Assertions.assertEquals("com/rcjava/scratch/Test.class",classes.get(0));
    }

    /**
     * Tests removing a class from a jar file
     */
    @Test
    public void testRemoveClass() throws IOException {
        JarEditor jarEditor = new JarEditor(scratchJar1.toFile()).load();
        jarEditor.removeClass("com/rcjava/scratch/Test.class");
        Assertions.assertFalse(jarEditor.hasClass("com/rcjava/scratch/Test.class"));
        Path tmp = Files.createTempFile("tmp", "jar");
        try {
            jarEditor.regenerate(tmp.toFile());
            JarEditor reloaded = new JarEditor(tmp.toFile()).load();
            Assertions.assertFalse(reloaded.containsClasses());
        } finally {
            Files.delete(tmp);
        }
    }

    /**
     * Tests removing a class from a signed jar file
     */
    @Test
    public void testRemoveClassSigned() throws IOException {
        JarEditor jarEditor = new JarEditor(signedJar.toFile()).load();
        IOException thrown = Assertions.assertThrows(IOException.class, () -> jarEditor.removeClass("com/rcjava/scratch/Test.class"));
        Assertions.assertEquals("This JAR file is digitally signed.",thrown.getMessage());
    }

    /**
     * Decompile a class from a jar file
     */
    @Test
    public void testDecompile() throws IOException {
        JarEditor jarEditor = new JarEditor(scratchJar1.toFile()).load();
        Path tmpDir = Files.createTempDirectory("tmp");
        jarEditor.decompileClass("com/rcjava/scratch/Test.class",tmpDir.toFile());
        Path decompiled = Paths.get(tmpDir.toFile().getAbsolutePath() + File.separator + "com/rcjava/scratch/Test.java");
        Assertions.assertTrue(Files.exists(decompiled));
    }

    /**
     * Tests the logic that verifies whether a jar is signed
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void testSignedCheck() throws IOException {
        JarEditor je = new JarEditor(signedJar.toFile()).load();
        Assertions.assertTrue(je.isSigned());
        je = new JarEditor(scratchJar1.toFile()).load();
        Assertions.assertFalse(je.isSigned());
    }

    /**
     * Tests adding a file to a signed JAR (should blow-up!)
     * @throws IOException - thrown for errors
     */
    @Test
    public void testAddClassSigned() throws IOException {
        JarEditor jarEditor = new JarEditor(signedJar.toFile()).load();
        Path targetFile = Files.createFile(Paths.get("Me.class"));
        Path regen = Files.createTempFile("pre","suf");
        try {
            IOException thrown = Assertions.assertThrows(IOException.class, () -> jarEditor.addFile("com/rcjava/foobar",targetFile.toFile()));
            Assertions.assertEquals("This JAR file is digitally signed.",thrown.getMessage());
        } finally {
            Files.deleteIfExists(targetFile);
            Files.deleteIfExists(regen);
        }
    }

    /**
     * Tests adding a file (which could be a class etc.)
     */
    @Test
    public void testAddFile() throws IOException {
        JarEditor jarEditor = new JarEditor(scratchJar1.toFile()).load();
        Path regen = Files.createTempFile("pre","suf");
        try {
            jarEditor.addFile("com/rcjava/foobar",clazz.toFile());
            jarEditor.regenerate(regen.toFile());
            JarEditor je = new JarEditor(regen.toFile()).load();
            Assertions.assertTrue(je.hasClass("com/rcjava/foobar/" + clazz.toFile().getName()));
        } finally {
            Files.deleteIfExists(regen);
        }
    }

    /**
     * Tests if a malformed JAR file can be identified
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void testMalformedJar() throws IOException {
        JarEditor jarEditor = new JarEditor(malformedJar.toFile()).load();
        Assertions.assertEquals(4,jarEditor.getDuplicates().size());
    }

    /**
     * Tests the same-file logic
     */
    @Test
    public void testSameFile() throws IOException {
        JarEditor jarEditor1 = new JarEditor(scratchJar1.toFile()).load();
        JarEditor jarEditor2 = new JarEditor(scratchJar1.toFile()).load();
        Assertions.assertTrue(jarEditor1.isSameFile(jarEditor2));
    }

    /**
     * Tests the version logic
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void testVersion() throws IOException {
        JarEditor jarEditor1 = new JarEditor(scratchJar1.toFile()).load();
        Assertions.assertEquals("55.0",jarEditor1.getVersion());
    }

    /**
     * Verifies that a fingerprint is generated
     */
    @Test
    public void testFingerPrint() throws IOException {
        JarEditor jarEditor = new JarEditor(scratchJar1.toFile()).load();
        String fingerPrint = jarEditor.fingerPrint();
        Assertions.assertNotNull(fingerPrint);
        Assertions.assertTrue(fingerPrint.length() > 5);
        JarEditor jarEditor2 = new JarEditor(scratchJar2.toFile()).load();
        Assertions.assertNotEquals(jarEditor.fingerPrint(),jarEditor2.fingerPrint());
    }

    /**
     * Tests the summary
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void testSummary() throws IOException {
        JarEditor jarEditor = new JarEditor(signedJar.toFile()).load();
        List<String[]> summary = jarEditor.getClassSummary();
        Assertions.assertTrue(summary.get(0)[0].compareTo(summary.get(summary.size() - 1)[0]) < 0);
        Assertions.assertNotEquals(summary.get(0)[1], summary.get(1)[1]);
        Assertions.assertTrue(summary.get(0)[1].length() > 1);
    }

    /**
     * Gets a dump of all of the imports used.
     */
    @Test
    public void testImportDump() throws IOException {
        JarEditor jarEditor = new JarEditor(signedJar.toFile(),true).load();
        Set<String> imports =  jarEditor.getImports();
        Assertions.assertFalse(imports.isEmpty());
    }

    /**
     * Tests the logic that retrieves all of the resources
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void testResources() throws IOException {
        JarEditor jarEditor = new JarEditor(signedJar.toFile()).load();
        List<String> resources = jarEditor.getResources();
        Assertions.assertEquals(3,resources.size());
    }

}
