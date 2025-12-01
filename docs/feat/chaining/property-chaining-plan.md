# Property chaining support plan

## Goal
Allow fluent criteria like `like -> like.getPost().getId()` so that nested
property chains can be used everywhere a `Property<T, R>` is currently accepted,
including `where`, `and`, `or`, `orderBy`, and `fetchJoin`.

## Constraints & observations
- `PropertyNameResolver` presently assumes method references with direct getter
  names, so lambda implementations (`lambda$0`) cannot be resolved.
- Spring Data Specifications already support nested property navigation using
  dot-separated paths (e.g., `post.id`).
- We need to keep existing behavior for simple method references and string
  paths, while extending the resolver to inspect chained getter invocations.
- Tests must cover single-depth (`post.id`) and deeper association chains (e.g.,
  `like.getPost().getAuthor().getTeam().getId()`), including interactions with
  fetch joins when required for lazy relationships.

## Implementation approach
1. Enhance `PropertyNameResolver.resolve` to inspect the serialized lambda and
   rebuild the chain of getter invocations. Strategy:
   - Use `SerializedLambda` metadata to identify simple getter references and
     short-circuit immediately when the method name already follows the getter
     convention.
   - For chain lambdas, a CGLIB-backed `PropertyPathRecorder` instantiates
     lightweight proxies (requires a non-final class with a non-private no-arg
     constructor) and tracks each getter invocation to build the dot-separated
     path plus the final return type.
   - Cache resolved paths per lambda signature to amortize proxy/reflective
     costs.
2. Document and enforce constraints:
   - Only JavaBean-style getter chains are supported; invoking arbitrary
     methods (`size()`, `toLowerCase()`, etc.) deliberately fails with a
     descriptive `IllegalArgumentException`.
   - Interfaces, arrays, or final classes cannot be proxied mid-chain and throw
     actionable errors.
   - Proxies call the target type's constructor once, so entities should expose
     side-effect-free no-arg constructors (as JPA already recommends).
3. Update `FieldStep` consumers to continue accepting nested `Property`
   references without additional changes (they already handle dot paths), but
   add documentation and tests covering failure cases and fetch-join
   interactions.
4. Tests:
   - Keep `src/test/java/me/miensoap/fluent/property/FluentQueryPropertyChainingTest.java`
     for positive scenarios (simple ID filter, deep associations, fetch joins).
   - Add failure-focused tests to ensure non-getter calls and unsupported
     structures surface helpful error messages.
5. Iterate until all tests pass.
