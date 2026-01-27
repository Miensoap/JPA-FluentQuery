package me.miensoap.fluent.core;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class PropertyNameResolver {

    private static final ConcurrentMap<LambdaCacheKey, ResolvedProperty> CACHE = new ConcurrentHashMap<>();

    private PropertyNameResolver() {
    }

    static String resolve(Property<?, ?> property) {
        return resolveInternal(property).path();
    }

    static Class<?> resolveType(Property<?, ?> property) {
        return resolveInternal(property).type();
    }

    static boolean isGetter(String methodName) {
        return methodName != null
            && (methodName.startsWith("get") && methodName.length() > 3
            || methodName.startsWith("is") && methodName.length() > 2);
    }

    static String propertyName(String getterName) {
        if (getterName.startsWith("get")) {
            return decapitalize(getterName.substring(3));
        }
        if (getterName.startsWith("is")) {
            return decapitalize(getterName.substring(2));
        }
        throw new IllegalArgumentException("Property references must point to a JavaBean getter: " + getterName);
    }

    private static SerializedLambda serializedLambda(Serializable lambda) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(lambda);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Unable to extract property information. " +
                "If using Kotlin, ensure the lambda is a method reference (e.g., Member::name). " +
                "Kotlin function literals are not supported.", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to extract property information", e);
        }
    }

    private static ResolvedProperty resolveInternal(Property<?, ?> property) {
        SerializedLambda lambda = serializedLambda(property);
        LambdaCacheKey key = LambdaCacheKey.from(lambda);
        return CACHE.computeIfAbsent(key, k -> resolveWithoutCache(property, lambda));
    }

    private static ResolvedProperty resolveWithoutCache(Property<?, ?> property, SerializedLambda lambda) {
        String methodName = lambda.getImplMethodName();
        if (isGetter(methodName)) {
            return new ResolvedProperty(propertyName(methodName), returnType(lambda));
        }
        Class<?> rootType = parameterType(lambda);
        return PropertyPathRecorder.capture(property, rootType, lambda);
    }

    private static Class<?> returnType(SerializedLambda lambda) {
        try {
            MethodType methodType = MethodType.fromMethodDescriptorString(
                lambda.getImplMethodSignature(),
                PropertyNameResolver.class.getClassLoader()
            );
            return methodType.returnType();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to resolve property type", e);
        }
    }

    private static Class<?> parameterType(SerializedLambda lambda) {
        try {
            MethodType methodType = MethodType.fromMethodDescriptorString(
                lambda.getImplMethodSignature(),
                PropertyNameResolver.class.getClassLoader()
            );
            if (methodType.parameterCount() == 0) {
                throw new IllegalArgumentException("Property reference lambda must declare a target parameter");
            }
            return methodType.parameterType(0);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to resolve property root type", e);
        }
    }

    private static String decapitalize(String name) {
        return Introspector.decapitalize(name);
    }

    private static final class LambdaCacheKey {

        private final String implClass;
        private final String implMethodName;
        private final String implMethodSignature;
        private final String instantiatedMethodType;

        private LambdaCacheKey(String implClass, String implMethodName, String implMethodSignature,
                               String instantiatedMethodType) {
            this.implClass = implClass;
            this.implMethodName = implMethodName;
            this.implMethodSignature = implMethodSignature;
            this.instantiatedMethodType = instantiatedMethodType;
        }

        static LambdaCacheKey from(SerializedLambda lambda) {
            return new LambdaCacheKey(
                lambda.getImplClass(),
                lambda.getImplMethodName(),
                lambda.getImplMethodSignature(),
                lambda.getInstantiatedMethodType()
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LambdaCacheKey that = (LambdaCacheKey) o;
            return Objects.equals(implClass, that.implClass)
                && Objects.equals(implMethodName, that.implMethodName)
                && Objects.equals(implMethodSignature, that.implMethodSignature)
                && Objects.equals(instantiatedMethodType, that.instantiatedMethodType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(implClass, implMethodName, implMethodSignature, instantiatedMethodType);
        }
    }
}
