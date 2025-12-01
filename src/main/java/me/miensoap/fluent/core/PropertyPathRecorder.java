package me.miensoap.fluent.core;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

final class PropertyPathRecorder {

    private PropertyPathRecorder() {
    }

    static ResolvedProperty capture(Property<?, ?> property, Class<?> rootType, SerializedLambda lambda) {
        Objects.requireNonNull(property, "Property reference must not be null");
        Objects.requireNonNull(rootType, "Property root type must not be null");
        Recorder recorder = new Recorder(rootType, lambda);
        recorder.record(property);
        return recorder.result();
    }

    private static final class Recorder implements MethodInterceptor {

        private final Class<?> rootType;
        private final String lambdaDescription;
        private final List<String> segments = new ArrayList<>();
        private Class<?> leafType;

        Recorder(Class<?> rootType, SerializedLambda lambda) {
            this.rootType = rootType;
            this.lambdaDescription = describeLambda(lambda);
        }

        void record(Property<?, ?> property) {
            Object proxy = createProxy(rootType);
            try {
                @SuppressWarnings("unchecked")
                Property<Object, Object> invokable = (Property<Object, Object>) property;
                invokable.apply(proxy);
            } catch (ClassCastException e) {
                throw fail("Unable to evaluate property reference", e);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (RuntimeException e) {
                throw fail("Property chaining failed while invoking getter chain", e);
            }
            if (segments.isEmpty()) {
                throw fail("Property references must invoke at least one getter");
            }
        }

        ResolvedProperty result() {
            return new ResolvedProperty(String.join(".", segments), leafType);
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return proxy.invokeSuper(obj, args);
            }
            if (!PropertyNameResolver.isGetter(method.getName())) {
                throw fail("Property chaining only supports getter methods but got '" + method.getName() + "'");
            }
            String property = PropertyNameResolver.propertyName(method.getName());
            segments.add(property);
            leafType = method.getReturnType();
            if (shouldProxy(leafType)) {
                return createProxy(leafType);
            }
            return defaultValue(leafType);
        }

        private Object createProxy(Class<?> type) {
            if (type == null) {
                throw fail("Cannot create proxy for null type");
            }
            if (type.isPrimitive()) {
                return defaultValue(type);
            }
            if (type.isInterface()) {
                throw fail("Property chaining does not support interface return types: " + type.getName());
            }
            if (Modifier.isFinal(type.getModifiers())) {
                throw fail("Property chaining requires non-final types but got: " + type.getName());
            }
            if (!hasUsableConstructor(type)) {
                throw fail("Property chaining requires a non-private no-arg constructor for " + type.getName());
            }
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(type);
            enhancer.setCallback(this);
            enhancer.setInterceptDuringConstruction(false);
            enhancer.setUseFactory(false);
            try {
                return enhancer.create();
            } catch (CodeGenerationException | IllegalArgumentException e) {
                throw fail("Property chaining could not create proxy for " + type.getName(), e);
            }
        }

        private boolean shouldProxy(Class<?> type) {
            if (type == null) {
                return false;
            }
            if (type.isPrimitive()) {
                return false;
            }
            if (type.isArray()) {
                return false;
            }
            if (type.isInterface()) {
                return false;
            }
            return !Modifier.isFinal(type.getModifiers());
        }

        private Object defaultValue(Class<?> type) {
            if (type == null || !type.isPrimitive()) {
                return null;
            }
            if (type == Boolean.TYPE) {
                return Boolean.FALSE;
            }
            if (type == Byte.TYPE) {
                return (byte) 0;
            }
            if (type == Short.TYPE) {
                return (short) 0;
            }
            if (type == Integer.TYPE) {
                return 0;
            }
            if (type == Long.TYPE) {
                return 0L;
            }
            if (type == Float.TYPE) {
                return 0F;
            }
            if (type == Double.TYPE) {
                return 0D;
            }
            if (type == Character.TYPE) {
                return '\0';
            }
            return null;
        }

        private boolean hasUsableConstructor(Class<?> type) {
            try {
                Constructor<?> ctor = type.getDeclaredConstructor();
                return !Modifier.isPrivate(ctor.getModifiers());
            } catch (NoSuchMethodException e) {
                return false;
            }
        }

        private IllegalArgumentException fail(String message) {
            return fail(message, null);
        }

        private IllegalArgumentException fail(String message, Throwable cause) {
            String detailed = message + " (lambda=" + lambdaDescription + ", path=" + describePath() + ")";
            return cause == null ? new IllegalArgumentException(detailed) : new IllegalArgumentException(detailed, cause);
        }

        private String describePath() {
            if (segments.isEmpty()) {
                return rootType.getName();
            }
            return rootType.getName() + "." + String.join(".", segments);
        }

        private String describeLambda(SerializedLambda lambda) {
            if (lambda == null) {
                return "<unknown>";
            }
            String implType = lambda.getImplClass() == null ? "unknown" : lambda.getImplClass().replace('/', '.');
            return implType + "#" + lambda.getImplMethodName();
        }
    }
}
