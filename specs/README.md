# Generated OpenAPI contract

This directory is for the **generated OpenAPI contract** used later by Apigee and 42Crunch.

1. Run the application locally (`.\gradlew.bat bootRun` on Windows, `./gradlew bootRun` on macOS/Linux).
2. In a second terminal, run `.\gradlew.bat generateOpenApiSpec` (Windows) or `./gradlew generateOpenApiSpec`.
3. The task retrieves the running application's `/v3/api-docs` and writes `specs/openapi.json`.
4. Review the generated file and commit it to Git when you are ready to establish a contract baseline.

For a non-default API URL, use:

```
.\gradlew.bat generateOpenApiSpec -PapiDocsUrl=http://localhost:8081/v3/api-docs
```

This is generation from the running application annotations, not a manually maintained static schema.
The generated JSON is NOT created until the application is running and the task is executed.
Do not publish the localhost server URL in externally shared specifications; adjust server details in subsequent phases.
