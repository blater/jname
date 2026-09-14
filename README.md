# jname

[![CI](https://github.com/blater/jname/actions/workflows/ci.yml/badge.svg)](https://github.com/blater/jname/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/blater/jname)](https://github.com/blater/jname/releases/latest)

Jname is a small Java 25 library and command-line program for generating
human-readable names and compact tokens. Its dictionary behavior follows
[goname v0.3.0](https://github.com/blater/goname/tree/25c4d921b86e10123d1e45b6d69da267cfb47256),
which is pinned as the immediate authority for all bundled word lists and
strategies.

## Examples

```console
$ jname
plausible-dace
$ jname -s _
foxy_squirrel
$ jname --ubuntu
vehement-vulture
$ jname --adjective
rapid
$ jname --strategy tolkien --mixedcase
ancient-Aragorn
$ jname --strategy hex
8d31
$ jname --strategy base32 --words 2
9f2c-j7wx
$ jname --strategy ulid
01arz3ndektsv4rrffq69g5fav
$ jname -p ticket -s _ --strategy tolkien
ticket_relevant_rose
```

## Install with Brew

Install the native macOS ARM64 executable with Homebrew:

```shell
brew install blater/tap/jname
```

## Command-line usage

```text
Usage: jname [-w|--words INT] [-l|--letters INT]
               [-s|--separator STR] [-p|--prefix STR] [-d|--dir STR]
               [-c|--complexity INT] [-t|--strategy STR] [-u|--ubuntu]
               [-m|--mixedcase]

  -w, --words INT       number of words; default: 2 (1 for token strategies)
  -l, --letters INT     word limit / hex-base32 width; ulid is fixed at 26
                         default: unlimited / 4; explicit 0, 1, or 2 becomes 3
  -s, --separator STR   separator between words; default: -
  -p, --prefix STR      prefix before the generated name
  -d, --dir DIR         custom word-list directory
  -c, --complexity INT  0=small, 1=medium, 2=large
  -t, --strategy STR    generation strategy: tolkien, hex, base32, ulid
  -u, --ubuntu          generate an alliterative name
  -m, --mixedcase       preserve dictionary case; uppercase generated tokens
      --adverb          generate one adverb
      --adjective       generate one adjective
      --name            generate one name word
  -h, --help            show this help
```

The legacy single-category flags retain their precedence: `--name` wins over
`--adjective`, which wins over `--adverb`. Help takes precedence over other
arguments. `--words` must be a positive integer when supplied. An explicitly
supplied `--letters` value of `0`, `1`, or `2` is raised to `3`, including for
hex and Base32. Omitting the option leaves the API width at zero. ULIDs always
use 26 characters regardless of `--letters`.

## Generation strategies

| Strategy | Default word count | Default width | Result |
| --- | ---: | ---: | --- |
| default | 2 | unlimited | Dictionary words |
| `tolkien` | 2 | unlimited | Tolkien character and place names |
| `hex` | 1 | 4 | Lowercase hexadecimal token |
| `base32` | 1 | 4 | Lowercase Crockford Base32 token |
| `ulid` | 1 | 26, fixed | ULID with timestamp and entropy |

The default dictionary grammar is a name, an adjective plus a name, then one
or more adverbs for additional words. Built-in Tolkien generation uses Tolkien
names and merges Tolkien modifiers with the small-list adverbs and adjectives.
For a custom dictionary directory, Tolkien mode reads complete lists from its
`tolkien` subdirectory without merging them with bundled modifiers. Default
strategy custom directories continue to use `adverbs.txt`, `adjectives.txt`,
and `names.txt` at the root or in the selected `small`, `medium`, or `large`
subdirectory. Each custom list contains one word per line.

`--complexity` selects a dictionary tier for the default strategy. A selected
complexity cannot be combined with any non-default strategy, including Tolkien,
hex, Base32, and ULID.

Hex and Base32 use width four when the API width is zero; a positive API
`maxLetters` sets their width exactly, including direct API widths of one or two.
The CLI clamps every explicitly supplied `--letters` value to at least three.
ULIDs ignore nonnegative API and CLI length settings and remain fixed at 26
characters. All token strategies accept explicit word counts and separators.
They bypass dictionary loading, so a custom word directory, category selection,
and alliteration do not affect token generation.

Dictionary output is lowercase by default. `-m` or `--mixedcase` preserves the
source spelling for dictionary words and emits uppercase tokens. The configured
separator is used between generated words or tokens.

Use `-p` or `--prefix` to put text before the generated result. The configured
separator is inserted once between the verbatim prefix and the generated
result. Prefixes do not count as words and are excluded from length filtering,
case conversion, and alliteration. An empty prefix adds no separator.

## Usage as a Java API

```java
import blater.jname.Complexity;
import blater.jname.Jname;
import blater.jname.JnameOptions;
import blater.jname.JnameStrategy;

String ordinaryName = Jname.generate();
String longerName = Jname.generate(3, "_");

String configuredName = Jname.generate(JnameOptions.builder()
        .words(3)
        .separator("_")
        .maxLetters(8)
        .complexity(Complexity.MEDIUM)
        .alliterate(true)
        .mixedCase(true)
        .build());

String prefixedUlid = Jname.generate(JnameOptions.builder()
        .strategy(JnameStrategy.ULID)
        .prefix("ticket")
        .build());
```

The immutable options default `words` value is now zero, which means “use the
selected strategy's default.” Thus `JnameOptions.defaults().getWords()` is
zero, and `.words(0)` also selects two words for dictionary strategies or one
token for token strategies. The ordinary default generated name remains two
words. Positive word counts, including an explicit count of two for a token
strategy, are used exactly. The `Jname.generate(int)` and
`Jname.generate(int, String)` convenience methods still require positive
counts. Custom dictionary output is lowercase by default; use `.mixedCase(true)`
to preserve its spelling.

`JnameGenerator` accepts a `RandomGenerator` for deterministic or
application-controlled randomness. Its no-argument constructor uses
`SecureRandom`. Seeded Java and Go generators are repeatable within their own
implementations when their inputs are controlled; a seed controls ULID
entropy, not the default clock, so ULIDs generated against real time are not
guaranteed to repeat across runs. Seeded Java and Go strings are not promised
to match.

ULIDs are monotonic within one live `JnameGenerator`. Calls in the same
millisecond increase, and a clock rollback retains the last emitted timestamp
and increments its entropy, so the encoded timestamp can temporarily lead wall
time. Each generator owns its sequence in memory: separate generator objects,
processes, or restarts do not coordinate, and no state is persisted. The shared
`Jname` facade uses its shared generator and therefore shares one sequence.
Allocation is serialized per generator, but concurrent callers may finish in a
different order than their ULIDs were allocated. Entropy overflow raises
`JnameException` without wrapping; generation can resume with fresh entropy
after the clock advances beyond the retained timestamp. Strict lexicographic
ordering applies to individual ULID tokens rendered in a consistent case.
Prefixes, case changes, separators, or multiple-token formatting do not promise
ordering of complete generated strings.

## Run the executable fat jar

Native executables for macOS ARM64, Linux x64, and Windows x64, plus a
platform-independent fat JAR, are published on the
[GitHub releases page](https://github.com/blater/jname/releases).

Run the fat JAR with JDK 25 or newer:

```shell
mvn package
java -jar target/jname-0.3.0-all.jar
java -jar target/jname-0.3.0-all.jar --words 3 --separator _
java -jar target/jname-0.3.0-all.jar --ubuntu --complexity 0
java -jar target/jname-0.3.0-all.jar --strategy base32 --words 2
```

The complete command-line interface is shown by `--help`.

## Build

The project requires JDK 25 and Maven 3.9 or newer.

```shell
mvn verify
```

Lombok is used only while compiling. The resulting JAR has no runtime
dependencies.

### GraalVM native executable

With a GraalVM 25 JDK containing Native Image installed:

```shell
./build
target/jname --words 3
```

Equivalently, run `mvn -Pnative package`. The native-image resource
configuration embeds the bundled dictionaries in the executable. The `build`
script locates the standard macOS GraalVM 25 installation automatically and
accepts `--skip-tests` or additional Maven arguments.

### Releases and Homebrew

GitHub Actions runs JVM and native-image builds on changes. A matching `vX.Y.Z`
tag builds and smoke-tests native executables on macOS ARM64, Linux x64, and
Windows x64, builds the fat JAR, generates `SHA256SUMS`, and attaches every
artifact to the GitHub release. After publishing, the workflow writes
`Formula/jname.rb` to [`blater/homebrew-tap`](https://github.com/blater/homebrew-tap).

Configure the source repository once with a fine-grained token that has
`Contents: Read and write` access to `blater/homebrew-tap`:

```shell
./actions-setup.sh
```

To create a release after committing all other changes:

```shell
./release.sh 1.0.0
```

The release script updates `pom.xml`, runs tests, commits the version, and
pushes the matching tag. The tag-driven workflow performs artifact and
Homebrew publication.

## Origin, attribution, and license

The immediate source and authority for every bundled dictionary is the
[goname v0.3.0 release](https://github.com/blater/goname/tree/25c4d921b86e10123d1e45b6d69da267cfb47256),
pinned to commit `25c4d921b86e10123d1e45b6d69da267cfb47256`. Jname bundles all
three categories from its `small`, `medium`, `large`, and `tolkien` tiers, so
builds and runtime do not depend on a sibling Go checkout. The petname
dictionaries retain their Apache-2.0 terms; the Tolkien compilation includes
its separate CC BY-SA 4.0 attribution and terms. Provenance and required license
materials are included in [NOTICE](NOTICE),
[the bundled word-list metadata](src/main/resources/blater/jname/words/UPSTREAM.md),
and the `LICENSES/` directory. These dictionary terms are separate from the
jname software license, which is MIT; see [LICENSE](LICENSE).
