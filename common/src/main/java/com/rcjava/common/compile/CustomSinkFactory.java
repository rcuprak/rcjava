package com.rcjava.common.compile;

import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom Sink Factory
 */
public class CustomSinkFactory implements OutputSinkFactory {

    /**
     * Destination directory
     */
    private File dest;

    /**
     * Flag indicating that there were errors
     */
    private boolean errors;

    /**
     * Creates a new custom sink factory
     * @param dest - destination directory
     */
    public CustomSinkFactory(File dest) {
        this.dest = dest;
    }

    @Override
    public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
        if (sinkType == SinkType.JAVA && collection.contains(SinkClass.DECOMPILED)) {
            return Arrays.asList(SinkClass.DECOMPILED, SinkClass.STRING);
        } else {
            return Collections.singletonList(SinkClass.STRING);
        }
    }

    Consumer<SinkReturns.Decompiled> dumpDecompiled = d -> {
        try {
            File dir = new File(dest.getAbsolutePath() + File.separator + d.getPackageName().replaceAll("\\.", "/"));
            if(!dir.mkdirs()) {
                throw new IOException("Unable to create " + dir.getAbsolutePath());
            }
            File file = new File(dir.getAbsolutePath() + "/" + d.getClassName() + ".java");
            if(!file.createNewFile()) {
                throw new IOException("Unable to create " + file.getAbsolutePath());
            }
            Files.writeString(file.toPath(),d.getJava());
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    /**
     * Returns the accepted sinke types
     * @param sinkType - sink type
     * @param sinkClass - sink class
     * @param <T> - param
     * @return sink
     */
    @Override
    public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
        if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
            return x -> dumpDecompiled.accept((SinkReturns.Decompiled) x);
        }
        return ignore -> {};
    }

    /**
     * Returns true if there were errors
     * @return - true for errors
     */
    public boolean hasErrors() {
        return errors;
    }
}
