package com.rcjava.common.products;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates a configuration for a product.
 * @author Ryan Cuprak
 */
public class Product {

    /**
     * Packages
     */
    private final Set<String> packages = new HashSet<>();

    /**
     * Configuration as found on the classpath
     */
    private final String config;

    /**
     * Description
     */
    private final String description;

    /**
     * Constructs a new product
     * @param config - configuration
     * @param description - description
     */
    public Product(String config, String description) {
        this.config = config;
        this.description = description;
    }

    /**
     * Loads the list of classes from a file
     */
    public Product init() {
        try(InputStream is = Product.class.getResourceAsStream(config)) {
            if (is != null) {
                String text = IOUtils.toString(is, StandardCharsets.UTF_8.name());
                String[] packages = text.split("\n");
                for(String pack : packages) {
                    pack = pack.trim();
                    if(!pack.isBlank()) {
                        this.packages.add(pack);
                    }
                }
            } else {
                throw new RuntimeException("Unable to load configuration file for " + description);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration file for " + description,e);
        }
        return this;
    }

    /**
     * Returns true if the package is part of this product
     * @param packageName - package
     * @return true if is part of it
     */
    public boolean isOwned(String packageName) {
        for(String pack : packages) {
            if(pack.contains(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a description for the product
     * @return description
     */
    public String getDescription() {
        return description;
    }
}