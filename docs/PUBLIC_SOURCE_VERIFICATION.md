# Public source verification — 0.19.0

Clean initial source snapshot; no development history. Includes production source/resources, Gradle configuration and wrapper, player documentation and existing CC0 with scope clarification in NOTICE.md. Excludes all private development documentation, diagnostic logs/screenshots, external references, extracted vanilla model definitions, local asset working sources/generators, test harnesses and editor files. Original development data is preserved privately.

Production source/resources are byte-identical to the prepared stable tree. Procedural geometry/texture generators in the private archive provide provenance evidence; no packaged extracted game art, music, third-party mod binaries or copied vanilla reference definitions were found. This is not a blanket claim of ownership of Witcher/Minecraft names or characters. See NOTICE.md for scope and third-party rights.

Java 21 Gradle build PASS (1m41s), all 10 tasks executed; eight release mixins verified. First clean Loom configuration emitted nonfatal dependency-source remapping warnings; compilation and release checks passed. No full GameTest or new client compatibility test. Prior four-profile Stage 19F results are historical RC evidence.

JAR contents match the previously prepared stable JAR byte-for-byte. No debug/test resources, nested JARs, reference files, personal paths or credential-pattern findings in the archive. All PNG metadata dictionaries are empty. Pattern scans are not a guarantee of detecting arbitrary secrets.

{
  "bytes": 7978609,
  "sha256": "309309a5d76f94ce08ceaf62cd35158cc99d6d781a058eb58cbd44deabf4eb8f",
  "entry_count": 1051,
  "changed_vs_local_stable": [],
  "excluded_suspicious_entries": [],
  "private_data_findings": [],
  "build": "PASS 1m41s"
}
