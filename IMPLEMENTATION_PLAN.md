# Goname v0.3.0 parity implementation plan

Status: implemented and reviewed on 2026-09-14. Scope is goname v0.3.0 parity
plus the agreed instance-local monotonic ULID extension.

Verification completed:

- JDK 25 `mvn verify`: 36 tests passed, no failures, errors, or skipped tests.
- All twelve dictionary files compared byte-for-byte with the pinned Go commit;
  packaged JAR contents checked against those resource files.
- Packaged JAR smoke checks and an external Java API consumer passed, including
  shared-facade ULID ordering and 1,000 ULIDs in each consistent output case.
- GraalVM 25 macOS ARM64 native build and smoke checks passed for every strategy,
  dictionary tier, custom Tolkien alliteration, casing, prefixes, lengths,
  validation, and 1,000 monotonic ULIDs in each consistent output case.
- CI/release strategy smoke coverage and native archive attribution packaging
  updated. Linux and Windows execution remains to be verified by CI.

## Scope and authority

Bring jname's library and CLI to feature parity with goname through v0.3.0,
pinned to commit `25c4d921b86e10123d1e45b6d69da267cfb47256`.
The local reference checkout is `../goname`; the upstream reference is
<https://github.com/blater/goname/tree/v0.3.0>.

One deliberate extension to Go v0.3.0 behavior is required: ULIDs must increase
monotonically within each live `JnameGenerator` instance, using in-memory state
only. There is no ordering guarantee across independent instances, processes,
or restarts. Dictionary authority remains unchanged.

**Goname's dictionaries are authoritative for every tier and strategy.**
Copy all twelve dictionary text files from the pinned Go revision: the three
categories (`adverbs.txt`, `adjectives.txt`, `names.txt`) under `small`, `medium`,
`large`, and `tolkien`. Preserve their exact bytes, spelling, casing, order, and
contents. Do not independently curate, normalize, or restore older petname copies.
Use petname and Tolkien Gateway references for provenance and attribution, not
as alternative sources of dictionary contents.

This includes goname v0.1.0's medium-list additions (`funky` and `man`), v0.2.0's
Tolkien strategy and locations, v0.2.1's ASCII Tolkien name compilation, and
v0.3.0's token strategies, prefixes, casing, and adapter dispatch.

Keep dictionaries bundled in jname. Builds and runtime must not require the
sibling Go checkout. Record hashes and provenance for the pinned copies so
future dictionary updates can deliberately track a new goname revision.

## 1. Establish the baseline and synchronize dictionaries

- Run the existing JVM tests against the current working tree. Incorporate the
  existing uncommitted CLI and generator refactoring without discarding it.
- Copy all authoritative dictionary files into
  `src/main/resources/blater/jname/words/` from the pinned revision.
- Update `WordListIntegrityTest` with hashes derived from those source files.
  Verify exact copies and assert that each medium category includes its small
  counterpart. Include Tolkien integrity and ASCII-name checks.
- Update dictionary metadata, README, and repository/bundled notices to name
  goname and its pinned revision as the immediate source. Replace claims that
  all dictionaries are unchanged petname copies.
- Carry forward the source attribution and separate dictionary license terms,
  including the Tolkien compilation's CC BY-SA 4.0 attribution and the petname
  dictionaries' Apache-2.0 materials. Ensure required materials ship in the JAR
  and distribution. Keep software licensing distinct from dictionary licensing.

## 2. Extend the API and introduce strategy adapters

- Add public `JnameStrategy` values `DEFAULT`, `TOLKIEN`, `HEX`, `BASE32`, and
  `ULID`; retain `JnameType` for dictionary categories.
- Add `strategy`, `prefix`, and `mixedCase` to immutable `JnameOptions`, with
  defaults `DEFAULT`, an empty prefix, and `false`.
- Change the option-level word count default to zero, meaning automatic:
  dictionary strategies resolve to two words and token strategies to one.
  Positive counts, including an explicit two for tokens, remain exact.
  Negative counts are invalid. Resolve defaults without mutating caller options.
- Preserve positive-count validation in `Jname.generate(int)` and
  `Jname.generate(int, String)`, and for explicit CLI `--words` values.
- Reject any non-default strategy combined with any non-default complexity,
  including token strategies. Retain existing separator and length validation.
- Introduce a package-private adapter interface with default-word-count and
  generation operations, selected by an explicit factory. Use composition:
  a shared dictionary adapter for default/Tolkien, a shared alphabet-token
  adapter for hex/Base32, and a separate ULID adapter. Keep implementations
  internal; public adapter registration is outside this parity scope.
- Keep `Jname` and `JnameGenerator` as public entry points. Preserve secure
  randomness by default, seeded generation, injected `RandomGenerator` support,
  and serialized access to random sources for concurrent callers.
- Apply a nonempty prefix once after adapter generation using the configured
  separator. Preserve the prefix verbatim; it is excluded from word counts,
  length filtering, case conversion, and alliteration. Empty prefixes add no
  separator, and empty separators concatenate directly.

## 3. Implement dictionary strategy behavior and casing

- Default generation retains the existing grammar: one name; adjective plus
  name for two words; preceding adverbs for additional words.
- Built-in Tolkien generation uses the authoritative Tolkien names and merges
  small-list modifiers with Tolkien modifiers. Match goname's exact-string
  deduplication and case-insensitive ordering with an original-spelling
  tiebreaker. Cache the resulting immutable lists. This runtime merge does not
  alter the copied resource files.
- With Tolkien and a custom directory, load complete lists from
  `<directory>/tolkien`; do not merge custom lists with bundled modifiers.
  Preserve default-strategy root and complexity-subdirectory behavior.
- Lowercase generated dictionary words by default; `mixedCase=true` preserves
  source spelling. Apply this to both complete names and individual categories.
  Use locale-independent casing in Java and test behavior under a non-English
  default locale.
- Compare initial Unicode code points case-insensitively for alliteration,
  independently of output casing. Retain code-point-based length filtering.

## 4. Implement tokens and ULIDs

| Strategy | Default count | Default token width | Alphabet/output |
| --- | --- | --- | --- |
| Hex | 1 | 4 | Hexadecimal, lowercase by default |
| Base32 | 1 | 4 | Crockford Base32, lowercase by default |
| ULID | 1 | 26, fixed | Timestamp and entropy encoded in Crockford Base32 |

- Use Crockford's alphabet `0123456789ABCDEFGHJKMNPQRSTVWXYZ`, excluding
  I, L, O, and U. `mixedCase=true` makes all token output uppercase.
- Positive API `maxLetters` sets hex/Base32 width exactly; zero selects width
  four. ULID ignores nonnegative length settings and always retains width 26.
- Respect explicit word counts and separators for every token strategy.
- Match goname by bypassing dictionary loading for tokens. `wordDirectory`,
  dictionary category selection, and alliteration have no effect in these
  strategies; document this behavior rather than introducing new rejections.
- Encode ULIDs using a 48-bit Unix millisecond timestamp and 80 bits of entropy
  from the configured random source. Preserve leading zeroes and the first
  character range `0` through `7`. No new runtime dependency is required.
- Allow an internal clock injection for deterministic tests; normal generators
  use current time. A random seed controls entropy, not the default clock.
- Make ULID generation monotonic by default, with no additional flag. Keep an
  initialized marker, the last emitted timestamp, and the last 80-bit entropy
  value in generator-owned memory. Retain this state across calls and strategy
  switches; do not recreate it when selecting an adapter. Independent generators
  have independent state, even when supplied the same random source.
- On the first ULID, or when the clock advances beyond the last timestamp, use
  the current timestamp and draw fresh entropy. If the clock equals or precedes
  the last timestamp, retain the last timestamp and increment its entropy as an
  unsigned 80-bit integer, carrying across bytes. This also preserves ordering
  during clock rollback; the encoded timestamp can temporarily lead wall time.
- If incrementing entropy would overflow, throw `JnameException` without
  wrapping, emitting an ID, or modifying the last successful state. Once the
  clock advances beyond the retained timestamp, generation can resume with fresh
  entropy. Reject timestamps outside the representable 48-bit range.
- Serialize clock sampling, entropy selection/increment, and state publication
  as one operation per generator. Successful ULIDs increase in that serialized
  allocation order; concurrent caller completion order is not guaranteed.
  Multiple ULID tokens in one result use the same generator state.
- The shared `Jname` facade uses its existing shared generator and therefore
  shares that sequence. Separate `JnameGenerator` objects, CLI processes, and
  restarts have no mutual ordering guarantee. No state is persisted.
- Strict lexicographic ordering applies to individual ULID tokens rendered in
  a consistent case. Changing case, prefixes, separators, or word counts does
  not promise lexical ordering of the resulting complete strings.

## 5. Update CLI and migration documentation

- Add `-t`/`--strategy` accepting exactly `tolkien`, `hex`, `base32`, and `ulid`;
  default strategy remains implicit. Add `-p`/`--prefix` and
  `-m`/`--mixedcase`.
- Retain existing flags, single-category precedence (`name`, then adjective,
  then adverb), help handling, and success/error output conventions.
- Preserve goname's actual CLI length behavior: an explicitly supplied
  `--letters 0`, `1`, or `2` is clamped to `3`, including for hex/Base32.
  Omitting the flag leaves the API value at zero. ULID remains fixed-width.
  Document the distinction from direct API widths of one and two.
- Update help and README with all strategies, default counts, length semantics,
  prefixes, casing, custom Tolkien directories, and invalid complexity pairings.
- Include Java builder examples and migration notes: `getWords()` on default
  options becomes zero; `.words(0)` selects a strategy default; custom dictionary
  output is now lowercase unless mixed case is enabled. The ordinary default
  generated name remains two words.
- Document that seeded Go and Java generators are repeatable within their own
  implementations, without promising identical cross-language seeded strings.
- Document the deliberate ULID extension: monotonicity is local to a live
  generator, including same-millisecond calls and clock rollback; it does not
  coordinate independent instances or processes. Describe overflow errors and
  distinguish ULID-token ordering from formatting of complete generated names.

## 6. Acceptance and packaging checks

- Verify default and explicit counts, category selection, separators, prefixes,
  casing, alliteration, custom directories, and validation across strategies.
- Test all four non-default strategies against each explicit complexity tier.
  Test negative API counts, automatic API counts, and rejected explicit CLI zero.
- Test CLI aliases, missing/unknown values, category precedence, help, exit codes,
  default token widths, and explicit length values zero through three.
- Use fixed clock/entropy ULID vectors to verify encoding, leading zeroes,
  timestamp boundaries within the 48-bit range, casing, and fixed width.
- Verify strict increases at a fixed millisecond, carry propagation across
  entropy bytes, fresh entropy after time advances, rollback handling, and
  overflow without state corruption followed by recovery when time advances.
  Test both consistent output cases, multi-token results, state retained across
  strategy switches, and isolation between generator instances.
- Exercise concurrent ULID allocation on one generator, verifying uniqueness
  and a contiguous increment sequence under a fixed clock. Test ordering of
  sequential calls; do not infer allocation order from concurrent completion.
  Exercise random-source synchronization when generators share a source.
- Compare Go/Java behavior through controlled dictionaries and random-source
  fixtures where useful; do not compare their language-specific seeded PRNG
  output as a parity requirement.
- Run `mvn verify` and smoke-test the packaged executable JAR for every strategy.
  Build and smoke-test the native executable where GraalVM is available and
  extend native CI coverage across supported release platforms.
- Confirm all authoritative dictionaries and attribution resources are packaged.
  The current native resource pattern covers the entire words directory; verify
  Tolkien generation from the built native executable.

Completion means all features through the pinned goname release and the agreed
instance-local monotonic ULID extension are implemented and verified, with
explicit migration notes and no dependency on a local Go checkout. Publishing
a release is a separate task.
