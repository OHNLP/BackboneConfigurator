package org.ohnlp.backbone.configurator.util;

import java.net.URL;
import java.net.URLClassLoader;

public class ModuleClassLoader extends URLClassLoader {
    public ModuleClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // Try local resolution first, then fall back to parent classloader on fail
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            return getParent().loadClass(name);
        }
    }

}
