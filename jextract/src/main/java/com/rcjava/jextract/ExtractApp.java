package com.rcjava.jextract;

import com.rcjava.common.JarEditor;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Extracts a class from a jar file
 * @author Ryan Cuprak
 */
@CommandLine.Command(description = "Extracts a source file from a jar file",
        name = "jextract", mixinStandardHelpOptions = true, version = "jextract 0.1")
public class ExtractApp implements Callable<Integer> {

    /**
     * JAR file we want to extract a class from
     */
    @CommandLine.Parameters(arity="1",description = "Class to be extracted")
    private String clazz;

    /**
     * JAR file we want to extract a class from
     */
    @CommandLine.Parameters(arity="2",description = "JAR File")
    private File jarFile;

    /**
     * Directory where file is to be extracted
     */
    @CommandLine.Parameters(arity="3",description = "JAR File")
    private File dir;

    /**
     * Main entry point
     * @param args - command line arguments
     */
    public static void main(String[] args)  {
        int exitCode = new CommandLine(new ExtractApp()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Invokes the utility
     * @return status code
     */
    public Integer call() {
        try {
            JarEditor jarInfo = new JarEditor(jarFile).load();
            jarInfo.decompileClass(clazz,dir);
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
