package com.rcjava.common.products;

import java.util.ArrayList;
import java.util.List;

/**
 * Products that we are interested in on the classpath.
 * @author Ryan Cuprak
 */
public class Products {

    /**
     * Singleton instance
     */
    private static Products instance;

    /**
     * Products
     */
    private final List<Product> products = new ArrayList<>(2);

    /**
     * Initializes the list of products
     */
    private void init() {
        products.add(new Product("/com/rcjava/common/products/JakartaEEPackages.txt","JakartaEE").init());
        products.add(new Product("/com/rcjava/common/products/JavaEEPackages.txt","JavaEE").init());
    }

    /**
     * Returns an instance of products
     * @return products
     */
    public synchronized static Products getInstance() {
        if(instance == null) {
            instance = new Products();
            instance.init();
        }
        return instance;
    }

    /**
     * Checks to see if the provided package name belongs to the product
     * @param packaging - packaging
     * @return list of products or null if none
     */
    public List<Product> belongs(String packaging) {
        List<Product> hits = null;
        for(Product product : products) {
            if(product.isOwned(packaging)) {
                if(hits == null) {
                    hits = new ArrayList<>(2);
                }
                hits.add(product);
            }
        }
        return hits;
    }
}