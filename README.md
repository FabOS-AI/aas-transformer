# aas-transformer

## Startup

Add the following parameter when starting the jar of application from cli:

```bash
java --add-opens=java.base/java.lang=ALL-UNNAMED ...
```

See also CMD in [Dockerfile](aas_transformer.service/Dockerfile) of transformer and [dev docs](docs/dev.md).