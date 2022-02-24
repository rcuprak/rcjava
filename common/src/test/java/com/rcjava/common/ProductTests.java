package com.rcjava.common;

import com.rcjava.common.products.Products;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks to see if products work
 * @author Ryan Cuprak
 */
public class ProductTests {

    /**
     * Instance of products
     */
    private Products products;

    /**
     * Initializes the products object
     */
    @BeforeEach
    public void init() {
        products = Products.getInstance();
    }

    @Test
    public void basicTest() {
        Assertions.assertNull(products.belongs("foobar"));
        Assertions.assertEquals("JakartaEE",products.belongs("jakartaee/servlet").get(0).getDescription());
    }

}
