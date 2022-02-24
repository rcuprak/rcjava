package com.rcjava.common;

import javassist.ClassPool;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Tests JarUtil methods
 * @author Ryan Cuprak
 */
public class JarUtilTests {

    /**
     * Classpath scratch
     */
    private static Path classpathScratch1;

    /**
     * Duplicate of 1
     */
    private static Path classpathScratch2;

    /**
     * POM 1
     */
    private static Path classpathScratchPOM1;

    /**
     * POM 2
     */
    private static Path classpathScratchPOM2;

    /**
     * Signed jar
     */
    private static Path signedJar;

    /**
     * Temp directory where we are doing the testing
     */
    private static Path tmpDir;

    /**
     * Preps the JAR files
     */
    @BeforeAll
    public static void beforeAll() throws IOException {
        classpathScratch1 = TestUtils.extract("/com/rcjava/common/jarutil/ClasspathScratch.jar");
        classpathScratch2 = TestUtils.extract("/com/rcjava/common/jarutil/ClasspathScratch.jar");
        classpathScratchPOM1 = TestUtils.extract("/com/rcjava/common/jarutil/ClasspathScratchPom1.jar");
        classpathScratchPOM2 = TestUtils.extract("/com/rcjava/common/jarutil/ClasspathScratchPom2.jar");
        signedJar = TestUtils.extract("/com/rcjava/common/jarutil/SignedJar.jar");
        tmpDir = Files.createTempDirectory("test");
        System.out.println("RDC - Signed: " + signedJar);
        InputStream is = JarUtilTests.class.getResourceAsStream("/com/rcjava/common/jar/commons-io-2.10.0.jar");
        if(is != null) {
            FileUtils.copyInputStreamToFile(is, signedJar.toFile());
        } else {
            throw new IOException("Unable to find JAR file commons-io-2.10.0.jar");
        }
    }

    /**
     * Cleans up afterwards
     */
    @AfterAll
    public static void afterAll() throws IOException {
        if(classpathScratch1 != null) {
            Files.delete(classpathScratch1);
        }
        if(classpathScratch2 != null) {
            Files.delete(classpathScratch2);
        }
        if(classpathScratchPOM1 != null) {
            Files.delete(classpathScratchPOM1);
        }
        if(classpathScratchPOM2 != null) {
            Files.delete(classpathScratchPOM2);
        }
        if(signedJar != null ){
            Files.delete(signedJar);
        }
        FileUtils.deleteDirectory(tmpDir.toFile());
    }

    /**
     * Tests that an exception is thrown if the exact same path is used (user probably didn't want to do that)
     */
    @Test
    public void verifySameFileException() {
        IOException thrown = Assertions.assertThrows(IOException.class, () -> {
            JarUtil.compare(classpathScratch1.toFile(),classpathScratch1.toFile());
        });
        Assertions.assertTrue(thrown.getMessage().startsWith("Comparing same exact file"));
    }

    /**
     * Compares the same jar file
     * @throws IOException - thrown if there is an error
     */
    @Test
    public void compareSameJar() throws IOException {
        Assertions.assertTrue(JarUtil.compare(classpathScratch1.toFile(),classpathScratch2.toFile()),"Files should be the same!");
    }

    /**
     * Compares a different jar - different number of files
     */
    @Test
    public void compareDifferentJar() throws IOException {
        Assertions.assertFalse(JarUtil.compare(classpathScratch1.toFile(),classpathScratchPOM1.toFile()));
    }

    /**
     * Compares a different jar - jar file has a pom.properties with a different timestamp
     */
    @Test
    public void compareJARWithSlightlyDifferentFile() throws IOException {
        Assertions.assertFalse(JarUtil.compare(classpathScratchPOM1.toFile(),classpathScratchPOM2.toFile()));
    }

    /**
     * Tests extracting the package
     */
    @Test
    public void testExtractPackage() {
        Assertions.assertEquals("com/test/ryan",JarUtil.extractPackage("com/test/ryan/Test.class"));
    }

    /**
     * Extracts and decompiles a class
     */
    @Test
    public void testExtractClassAndDecompile() throws IOException {
        File clazzDir = Files.createTempDirectory("clazz").toFile();
        File srcDir = Files.createTempDirectory("src").toFile();
        try {
            JarUtil.extractClass("com/rcjava/scratch/Test", clazzDir, classpathScratch1.toFile().getAbsolutePath());
            File targetClazz = new File(clazzDir.getAbsoluteFile() + File.separator + "com" + File.separator + "rcjava" + File.separator + "scratch" + File.separator + "Test.class");
            Assertions.assertTrue(targetClazz.exists());
            JarUtil.decompileClass(srcDir, targetClazz.getAbsolutePath());
            File targetSrc = new File(srcDir.getAbsoluteFile() + File.separator + "com" + File.separator + "rcjava" + File.separator + "scratch" + File.separator + "Test.java");
            Assertions.assertTrue(targetSrc.exists());
        } finally {
            FileUtils.deleteDirectory(clazzDir);
            FileUtils.deleteDirectory(srcDir);
        }
    }

    /**
     * Tests the logic for verifying if a JAR is signed
     */
    @Test
    public void testSigning() throws IOException {
        Assertions.assertFalse(JarUtil.checkSigned(classpathScratch1.toFile()));
        Assertions.assertTrue(JarUtil.checkSigned(signedJar.toFile()));
    }

    /**
     * Tests unsigning without overwriting
     */
    @Test
    public void testUnsignWithoutOverwrite() throws Exception {
        System.out.println("Unsigned: " + signedJar);
        Path unsignedJar = JarUtil.unsignJar(signedJar,false);
        Assertions.assertFalse(JarUtil.checkSigned(unsignedJar.toFile()));
    }

    /**
     * Tests the logic which extracts imports
     * @throws Exception - thrown if there is an error
     */
    @Test
    public void testGetImports() throws Exception {
        ClassPool cp = ClassPool.getDefault();
        Collection classes = cp.get("com.rcjava.common.JarUtil").getRefClasses();
        Assertions.assertEquals(45,classes.size());
    }
}
