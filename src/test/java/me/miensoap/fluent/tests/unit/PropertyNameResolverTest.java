package me.miensoap.fluent.tests.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.miensoap.fluent.core.PropertyResolverAccessor;

class PropertyNameResolverTest {

    @Test
    @DisplayName("단일 getter 람다는 path 와 타입을 정확히 추출한다")
    void resolvesSimpleGetterReference() {
        assertThat(PropertyResolverAccessor.resolve(DemoRoot::getName)).isEqualTo("name");
        assertThat(PropertyResolverAccessor.resolveType(DemoRoot::getName)).isEqualTo(String.class);
    }

    @Test
    @DisplayName("boolean getter 와 대문자 필드명도 규약대로 처리한다")
    void resolvesBooleanAndUpperCaseGetters() {
        assertThat(PropertyResolverAccessor.resolve(DemoRoot::isActive)).isEqualTo("active");
        assertThat(PropertyResolverAccessor.resolve(DemoRoot::getURL)).isEqualTo("URL");
    }

    @Test
    @DisplayName("getter 가 아닌 메서드 참조는 명확한 예외를 던진다")
    void rejectsNonGetterMethodReference() {
        assertThatThrownBy(() -> PropertyResolverAccessor.resolve(DemoRoot::nonGetter))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("root type");
    }

    @Test
    @DisplayName("람다 체인을 해석해 dot-path 와 leaf 타입을 반환한다")
    void resolvesLambdaChain() {
        assertThat(PropertyResolverAccessor.<DemoRoot, Long>resolve(root -> root.getChild().getId())).isEqualTo("child.id");
        assertThat(PropertyResolverAccessor.<DemoRoot, Long>resolveType(root -> root.getChild().getId())).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("인터페이스 타입이나 final 타입이 체인 중간에 있으면 예외가 발생한다")
    void failsOnUnsupportedIntermediateTypes() {
        Throwable interfaceError = org.assertj.core.api.Assertions.catchThrowable(
            () -> PropertyResolverAccessor.<DemoRoot, String>resolve(root -> root.getInterfaceChild().getValue())
        );
        assertThat(interfaceError)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("interface");

        Throwable finalError = org.assertj.core.api.Assertions.catchThrowable(
            () -> PropertyResolverAccessor.<DemoRoot, String>resolve(root -> root.getFinalChild().getValue())
        );
        assertThat(finalError)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("finalChild");
    }

    @Test
    @DisplayName("기본 생성자가 없는 타입을 만나면 명시적으로 실패한다")
    void failsWhenNoDefaultConstructor() {
        assertThatThrownBy(() -> PropertyResolverAccessor.<DemoRoot, String>resolve(root -> root.getNoDefaultChild().getValue()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("constructor");
    }

    @Test
    @DisplayName("같은 람다를 여러 번 resolve 해도 일관된 결과를 돌려준다")
    void cachesResolvedLambda() {
        String first = PropertyResolverAccessor.resolve(DemoRoot::getName);
        String second = PropertyResolverAccessor.resolve(DemoRoot::getName);
        assertThat(first).isEqualTo(second);
    }

    public static class DemoRoot {

        private final DemoChild child = new DemoChild();
        private final InterfaceChild interfaceChild = () -> "value";
        private final FinalChild finalChild = new FinalChild();

        DemoRoot() {
        }

        String getName() {
            return "demo";
        }

        boolean isActive() {
            return true;
        }

        String getURL() {
            return "url";
        }

        String nonGetter() {
            return "noop";
        }

        DemoChild getChild() {
            return child;
        }

        InterfaceChild getInterfaceChild() {
            return interfaceChild;
        }

        FinalChild getFinalChild() {
            return finalChild;
        }

        NoDefaultChild getNoDefaultChild() {
            return new NoDefaultChild("x");
        }
    }

    static class DemoChild {
        DemoChild() {
        }

        Long getId() {
            return 1L;
        }
    }

    private interface InterfaceChild {
        String getValue();
    }

    private static final class FinalChild {
        FinalChild() {
        }

        String getValue() {
            return "value";
        }
    }

    private static class NoDefaultChild {
        NoDefaultChild(String ignored) {
        }

        String getValue() {
            return "value";
        }
    }
}
