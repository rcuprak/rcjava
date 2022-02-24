package com.rcjava.common;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestUtils {

    /**
     * Extracts a resource out
     * @param resourcePath - resource path
     * @return Path to the file
     * @throws IOException - thrown if there are any errors
     */
    public static Path extract(String resourcePath) throws IOException {
        return extract(resourcePath,".bin");
    }

    /**
     * Extracts a resource out
     * @param resourcePath - resource path
     * @param ext - extension
     * @return Path to the file
     * @throws IOException - thrown if there are any errors
     */
    public static Path extract(String resourcePath, String ext) throws IOException {
        Path file = Files.createTempFile("tst", ext);
        try (InputStream is = TestUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Unable to load non-JAR");
            }
            FileUtils.copyInputStreamToFile(is, file.toFile());
        }
        return file;
    }

}
