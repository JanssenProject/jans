package io.jans.service.custom.script.jit;


public class DiscardableClassLoader {
    public static <T> Class<? extends T> classFromBytes(final Class<T> baseClass, final String name, final byte[] bytecode) {
        return new ClassLoader(baseClass.getClassLoader()) {
            Class<? extends T> c = defineClass(name, bytecode, 0, bytecode.length).asSubclass(baseClass);
        }.c;
    }
}
