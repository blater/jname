# jname

[![CI](https://github.com/blater/jname/actions/workflows/ci.yml/badge.svg)](https://github.com/blater/jname/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/blater/jname)](https://github.com/blater/jname/releases/latest)

Jname is a small Java 25 implementation based on
[Dustin Kirkland's petname](https://github.com/dustinkirkland/petname). It generates human-readable
random names from adverbs, adjectives, and animal names and can be used as either a library or an
executable JAR.

## Java API

```java
import blater.jname.Jname;

String name = Jname.generate();
String longerName = Jname.generate(3, "_");
```

For additional options:

```java
import blater.jname.Complexity;
import blater.jname.Jname;
import blater.jname.JnameOptions;

String name = Jname.generate(JnameOptions.builder()
        .words(3)
        .separator("_")
        .maxLetters(8)
        .complexity(Complexity.MEDIUM)
        .alliterate(true)
        .build());
```

`JnameGenerator` accepts a `RandomGenerator` when deterministic or application-controlled
randomness is required. Its no-argument constructor uses `SecureRandom`.

## Command line

Install the native macOS ARM64 executable with Homebrew:

```shell
brew install blater/tap/jname
```

Native executables for macOS ARM64, Linux x64, and Windows x64, plus a platform-independent fat
JAR, are published on the [GitHub releases page](https://github.com/blater/jname/releases).

Run the fat JAR with JDK 25 or newer:

```shell
mvn package
java -jar target/jname-0.1.0-SNAPSHOT-all.jar
java -jar target/jname-0.1.0-SNAPSHOT-all.jar --words 3 --separator _
java -jar target/jname-0.1.0-SNAPSHOT-all.jar --ubuntu --complexity 0
```

The executable supports the upstream options `--words`, `--letters`, `--separator`, `--dir`,
`--complexity`, `--ubuntu`, `--adverb`, `--adjective`, and `--name`, together with their upstream
short aliases where applicable. Run it with `--help` for the complete usage text.

Custom dictionary directories contain `adverbs.txt`, `adjectives.txt`, and `names.txt`, with one
word per line. When a complexity is selected, those files are read from a corresponding `small`,
`medium`, or `large` subdirectory.

## Build

The project requires JDK 25 and Maven 3.9 or newer.

```shell
mvn verify
```

Lombok is used only while compiling. The resulting JAR has no runtime dependencies.

### GraalVM native executable

With a GraalVM 25 JDK containing Native Image installed:

```shell
./build
target/jname --words 3
```

Equivalently, run `mvn -Pnative package`. The native-image resource configuration embeds the
built-in dictionaries in the executable. The `build` script locates the standard macOS GraalVM 25
installation automatically and accepts `--skip-tests` or additional Maven arguments.

### Releases and Homebrew

GitHub Actions performs ordinary JVM and native-image builds on every change. A matching `vX.Y.Z`
tag builds and smoke-tests native executables on macOS ARM64, Linux x64, and Windows x64, builds the
fat JAR, generates `SHA256SUMS`, and attaches every artifact to the GitHub release. After publishing
the release, the workflow writes `Formula/jname.rb` to
[`blater/homebrew-tap`](https://github.com/blater/homebrew-tap).

Configure the source repository once with a fine-grained token that has `Contents: Read and write`
access to `blater/homebrew-tap`:

```shell
./actions-setup.sh
```

To create a release after committing all other changes:

```shell
./release.sh 1.0.0
```

The release script updates `pom.xml`, runs tests, commits the version, and pushes the matching tag.
The tag-driven workflow performs all artifact and Homebrew publication.

## Origin, attribution, and license

This project is based on the design and shell implementation of
[dustinkirkland/petname](https://github.com/dustinkirkland/petname). Its bundled small, medium, and
large dictionaries are copied from upstream petname 2.11 commit
[`70ed924cb96c290ac051b8ae797417c4adbbb5c9`](https://github.com/dustinkirkland/petname/commit/70ed924cb96c290ac051b8ae797417c4adbbb5c9).

Both upstream petname and Jname are licensed under the
[Apache License, Version 2.0](LICENSE). Attribution and dictionary provenance are recorded in
[NOTICE](NOTICE) and in the bundled word-list metadata.
