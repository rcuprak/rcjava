package com.rcjava.jinfo;

import com.rcjava.common.JarEditor;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * This application provides information on jar files
 * @author Ryan Cuprak
 */
@CommandLine.Command(description = "Provides information on editing/querying jar files.",
        name = "em", mixinStandardHelpOptions = true, version = "jinfo 0.1")
public class JarInfoApp implements Callable<Integer> {

    /**
     * Version of the class files in the jar
     */
    @CommandLine.Option(names = {"-v", "--version"}, description = "Dumps the class version.")
    private boolean version;

    /**
     * Version of the class files in the jar
     */
    @CommandLine.Option(names = {"-c", "--count"}, description = "Prints the number of classes")
    private boolean count;

    /**
     * List all the classes
     */
    @CommandLine.Option(names = {"-l", "--list"}, description = "Lists all of the classes in the jar file")
    private boolean listClasses;

    /**
     * Flag indicating that the file should be cleared
     */
    @CommandLine.Option(names = {"-r", "--resources"}, description = "Lists all the resources in the file")
    private boolean listResources;

    /**
     * Prints the fingerprints for all of the classes and the jar file
     */
    @CommandLine.Option(names = {"-f", "--fingerprints"}, description = "Includes the fingerprints of all of the jars.")
    private boolean fingerPrints;

    /**
     * Flag indicating that the file should be cleared
     */
    @CommandLine.Option(names = {"-p", "--packages"}, description = "Lists all of the Java packages")
    private boolean listPackages;

    /**
     * Flag indicating that we want a list of all the imports
     */
    @CommandLine.Option(names = {"-i", "--imports"}, description = "Lists all of the Java classes that are imported")
    private boolean imports;

    /**
     * JAR file we want to query
     */
    @CommandLine.Parameters(arity="1",description = "JAR File")
    private File jarFile;

    /**
     * Main entry point
     * @param args - command line arguments
     */
    public static void main(String[] args)  {
        int exitCode = new CommandLine(new JarInfoApp()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Invokes the utility
     * @return status code
     */
    public Integer call() {
        try {
            JarEditor jarInfo = new JarEditor(jarFile).load();
            if(count) {
                System.out.println("Class count: " + jarInfo.getClassCount());
            }
            if(version) {
                System.out.println("Class version: " + jarInfo.getVersion());
            }
            if(listClasses) {
                System.out.println("Classes: ");
                List<String> clazzes = jarInfo.getClasses();
                if(clazzes.isEmpty()) {
                    System.out.println("No class files");
                } else {
                    if (fingerPrints) {
                        List<String[]> summaries = jarInfo.getClassSummary();
                        for(String[] summary : summaries) {
                            System.out.println(summary[0] + " " + summary[1]);
                        }
                    } else {
                        for (String clazz : jarInfo.getClasses()) {
                            System.out.println(clazz);
                        }
                    }

                }
            }
            if(listResources) {
                System.out.println("Resources: ");
                for(String rsc : jarInfo.getResources()) {
                    System.out.println(rsc);
                }
            }
            if(listPackages) {
                System.out.println("Packages: ");
                for(String pack : jarInfo.getPackages()) {
                    System.out.println(pack);
                }
            }
            if(imports) {
                System.out.println("Imports: ");
                for(String imp : jarInfo.getImports()) {
                    System.out.println(imp);
                }
            }
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
