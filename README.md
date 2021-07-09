# Fabric Mod Template
A quick mod template for one to use.

## Configuration
- [`gradle.properties`](./gradle.properties) - All the project properties.
- [`.internal/license-header.java`](./.internal) - The license header. None by default.
- [`.internal/spotless.importorder`](./.internal/spotless.importorder) - The import order. Config provided is `*`, `java`, `javax` then static.
- [`.internal/spotless.xml`](./.internal) - The Eclipse formatter config. Uses Eclipse's default if none is provided.
- [Issue Templates](./.github/ISSUE_TEMPLATE) - All the issue templates. Replace `<Mod>` with your mod name.
- [Workflows](./.github/workflows) - All the action workflows.