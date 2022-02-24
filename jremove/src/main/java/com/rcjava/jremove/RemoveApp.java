package com.rcjava.jremove;

import com.rcjava.common.JarEditor;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Extracts a class from a jar file
 * @author Ryan Cuprak
 */
@CommandLine.Command(description = "Removes a file from a JAR file",
        name = "jremove", mixinStandardHelpOptions = true, version = "jremove 0.1")
public class RemoveApp implements Callable<Integer> {

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
     * Main entry point
     * @param args - command line arguments
     */
    public static void main(String[] args)  {
        int exitCode = new CommandLine(new RemoveApp()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Invokes the utility
     * @return status code
     */
    public Integer call() {
        try {
            JarEditor jarInfo = new JarEditor(jarFile).load();
            jarInfo.removeClass(path);
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}