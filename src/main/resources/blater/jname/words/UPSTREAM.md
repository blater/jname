# Dictionary provenance

All twelve bundled dictionary text files are copied byte-for-byte from
[goname v0.3.0](https://github.com/blater/goname/tree/v0.3.0), pinned to commit
`25c4d921b86e10123d1e45b6d69da267cfb47256`. The Java runtime uses these copies
and does not read from the sibling Go repository.

The `small`, `medium`, and `large` lists in goname originate from Dustin
Kirkland's [petname](https://github.com/dustinkirkland/petname) 2.11, commit
`70ed924cb96c290ac051b8ae797417c4adbbb5c9`, and are distributed under the
Apache License 2.0. The authoritative goname snapshot includes its medium-list
additions (`funky` and `man`); the files here preserve that snapshot exactly.
The applicable Apache license is included in `LICENSES/Apache-2.0.txt` at the
repository root and `META-INF/LICENSES/Apache-2.0.txt` in the JAR.

The Tolkien name compilation and its themed adjective/adverb files are copied
from the same pinned goname revision. The name compilation is attributed to
[Tolkien Gateway](https://tolkiengateway.net/) and is licensed under Creative
Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0). Its source
indexes and license are described in [tolkien/README.md](tolkien/README.md);
the license notice is included in `LICENSES/CC-BY-SA-4.0.txt` at the
repository root and `META-INF/LICENSES/CC-BY-SA-4.0.txt` in the JAR.

The source files are not normalized or independently curated. The hashes below
are SHA-256 digests of the copied files:

| Dictionary file | SHA-256 |
| --- | --- |
| `small/adverbs.txt` | `f22baacc9c281e6e13daea8dcc8d204c5ad08b26d0c4cd5a1ea0e6e1e782b445` |
| `small/adjectives.txt` | `5f934ce94a217b85aec434d6803c6fc5b74628d10d83c4d0ddfdec9256e062b0` |
| `small/names.txt` | `a15d352808bda5b983dad1d68d71f97dd5819c28fe5779169e8ce9c82f0aff6b` |
| `medium/adverbs.txt` | `c19e6fc3d06acf6d5a3812772f60012e443cc96f41e63c5e88e0007e7111646c` |
| `medium/adjectives.txt` | `3b91020cc035b00b912f7cfb0b77a66a8465ee1e3dd1880b246699b57e703f2b` |
| `medium/names.txt` | `2d929c68b1e1e431ab7eff914514e8ea7f70d05e54d2fb3d9a9d6c90deb7a0c9` |
| `large/adverbs.txt` | `95b8d19579711acc199a3a6a6e102d5abd3d2df57bc052166d6009f651241591` |
| `large/adjectives.txt` | `8a36e132c66fc2a879770dfc1f73f2a11b454194ca75f3f0fde3e6922965608c` |
| `large/names.txt` | `6e0db1b0619462388115fb2ae26277db4391052f6161842c15f8d8189078a14c` |
| `tolkien/adverbs.txt` | `04c531229d1b26f31254b581f04fd4b053e37f5e5ecb01e71743522205593127` |
| `tolkien/adjectives.txt` | `ac7a58038f0be2b98af1358fd8d74e11438b1fbbca54a06ad904b5f95e7792a4` |
| `tolkien/names.txt` | `29d226288a0552229efc8bcb39d06d0e29bac92aff22175c40879eb66b511350` |
