This project uses a verified portable bootstrap `gradlew` / `gradlew.bat` in place of the official Gradle Wrapper binary, because no official `gradle-wrapper.jar` is bundled here. The launcher downloads Gradle 8.14.6 and checks the binary ZIP against Gradle's published SHA-256 checksum.

If you prefer standard Gradle Wrapper files after installing Gradle, run:
`gradle wrapper --gradle-version 8.14.6 --distribution-type bin`
Then commit the generated official `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`, and generated `gradlew` scripts.
