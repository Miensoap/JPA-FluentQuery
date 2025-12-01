package me.miensoap.fluent.core;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class PropertyNameResolver {

    private PropertyNameResolver() {
    }

    static String resolve(Property<?, ?> property) {
        SerializedLambda lambda = serializedLambda(property);
        String implMethod = lambda.getImplMethodName();
        if (implMethod.startsWith("get") && implMethod.length() > 3) {
            return decapitalize(implMethod.substring(3));
        }
        if (implMethod.startsWith("is") && implMethod.length() > 2) {
            return decapitalize(implMethod.substring(2));
        }
        throw new IllegalArgumentException("Property references must point to a JavaBean getter: " + implMethod);
    }

    static Class<?> resolveType(Property<?, ?> property) {
        SerializedLambda lambda = serializedLambda(property);
        String signature = lambda.getImplMethodSignature();
        String returnDescriptor = signature.substring(signature.indexOf(')') + 1);
        return toClass(returnDescriptor);
    }

    private static SerializedLambda serializedLambda(Serializable lambda) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(lambda);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to extract property information", e);
        }
    }

    private static String decapitalize(String name) {
        return Introspector.decapitalize(name);
    }

    private static Class<?> toClass(String descriptor) {
        switch (descriptor) {
            case "I":
                return Integer.TYPE;
            case "J":
                return Long.TYPE;
            case "S":
                return Short.TYPE;
            case "B":
                return Byte.TYPE;
            case "F":
                return Float.TYPE;
            case "D":
                return Double.TYPE;
            case "Z":
                return Boolean.TYPE;
            case "C":
                return Character.TYPE;
            case "V":
                return Void.TYPE;
            default:
                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                    String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("Unable to resolve property type: " + className, e);
                    }
                }
                throw new IllegalArgumentException("Unsupported method descriptor: " + descriptor);
        }
    }
}
