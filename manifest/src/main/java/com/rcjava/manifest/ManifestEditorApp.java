package com.rcjava.manifest;

import com.rcjava.common.ManifestEditor;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The manifest editor enables extraction of the jars within a manifest.
 * @author Ryan Cuprak
 */
@CommandLine.Command(description = "Provides editing of manifest classpath JARs.",
        name = "em", mixinStandardHelpOptions = true, version = "em 0.1")
public class ManifestEditorApp implements Callable<Integer> {

    /**
     * Jars to add the manifest
     */
    @CommandLine.Option(names = {"-a", "--add"}, description = "JARs to add to the manifest", split=",")
    private List<String> add;

    /**
     * Jars to be removed from the manifest
     */
    @CommandLine.Option(names = {"-r", "--remove"}, description = "JARs to remove from the manifest", split=",")
    private List<String> remove;

    /**
     * Flag indicating that the file should be cleared
     */
    @CommandLine.Option(names = {"-c", "--clear"}, description = "Clears the classpath from the manifest")
    private boolean clear;

    /**
     * Name of the new jar file
     */
    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the new jar file")
    private String name;

    /**
     * JAR file
     */
    @CommandLine.Parameters(arity="1",description = "JAR File")
    private File jarFile;

    /**
     * Main entry point
     * @param args - command line arguments
     */
    public static void main(String[] args)  {
        int exitCode = new CommandLine(new ManifestEditorApp()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Invokes the utility
     * @return status code
     */
    public Integer call() {
        try {
            ManifestEditor ma = ManifestEditor.createEditor(jarFile);
            if(clear) {
                ma.clearClasspath();
            }
            if(add != null) {
                ma.removeJars(add);
            }
            if(remove != null) {
                ma.removeJars(remove);
            }
            File output;
            if(name != null && name.length() > 0) {
                output = new File(name);
                if(!output.createNewFile()) {
                    System.err.println("Unable to create file " + output.getAbsolutePath());
                    return 1;
                }
            } else {
                output = jarFile;
            }
            ma.regenerate(output);
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
