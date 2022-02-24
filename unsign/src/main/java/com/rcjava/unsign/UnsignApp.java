package com.rcjava.unsign;

import com.rcjava.common.JarUtil;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

/**
 * Handles Unsigning a jar file
 * @author Ryan Cuprak
 */
@CommandLine.Command(description = "Removes the digital signatures from a JAR file.",
        name = "unsign", mixinStandardHelpOptions = true, version = "unsign 0.1")
public class UnsignApp {

    /**
     * Flag indicating that we want a list of all the imports
     */
    @CommandLine.Option(names = {"-o", "--overwrite"}, description = "Overwrite the original file.")
    private boolean overwrite;

    /**
     * JAR file we want to query
     */
    @CommandLine.Parameters(arity="1",description = "JAR File")
    private File jarFile;


    /**
     * Invokes the utility
     * @return status code
     */
    public Integer call() {
        try {
            JarUtil.unsignJar(jarFile.toPath(),overwrite);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return 1;
        }
        return 0;
    }


    /**
     * Main entry point
     * @param args - command line arguments
     */
    public static void main(String[] args)  {
        int exitCode = new CommandLine(new UnsignApp()).execute(args);
        System.exit(exitCode);
    }
}
