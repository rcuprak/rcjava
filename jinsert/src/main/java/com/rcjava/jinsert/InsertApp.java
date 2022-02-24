package com.rcjava.jinsert;

import com.rcjava.common.JarEditor;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Extracts a class from a jar file
 * @author Ryan Cuprak
 */
@CommandLine.Command(description = "Inserts a class or resource into a jar file",
        name = "jinsert", mixinStandardHelpOptions = true, version = "jinsert 0.1")
public class InsertApp implements Callable<Integer> {

    /**
     * JAR file we want to extract a class from
     */
    @CommandLine.Parameters(arity="1",description = "Package/directory in the JAR file.")
    private String path;

    /**
     * JAR file we want to extract a class from
     */
    @CommandLine.Parameters(arity="2",description = "JAR to be manipulated.")
    private File jarFile;

    /**
     * Directory where file is to be extracted
     */
    @CommandLine.Parameters(arity="3",description = "File to be inserted into the JAR")
    private File file;

    /**
     * Main entry point
     * @param args - command line arguments
     */
    public static void main(String[] args)  {
        int exitCode = new CommandLine(new InsertApp()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Invokes the utility
     * @return status code
     */
    public Integer call() {
        try {
            JarEditor jarInfo = new JarEditor(jarFile).load();
            jarInfo.addFile(path,file);
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
