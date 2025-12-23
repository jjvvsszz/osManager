plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "4.0.5"
    id("jacoco")
}

group = "tk.jaooo"
version = "v1.0"
description = "osManager"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
    // --- CORE SPRING BOOT ---
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-tomcat")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jsoup:jsoup:1.21.2")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // --- UTILITÁRIOS ---
    implementation("commons-codec:commons-codec")
    implementation("org.springframework.retry:spring-retry:2.0.12")
    implementation("org.springframework:spring-aspects")

    // --- ORACLE CLOUD (OCI SDK) ---
    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:3.77.2"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")
    implementation("com.oracle.oci.sdk:oci-java-sdk-secrets")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common")
    implementation("com.oracle.oci.sdk:oci-java-sdk-identity")
    implementation("com.oracle.oci.sdk:oci-java-sdk-vault")

    // --- SEGURANÇA JWT ---
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // --- DATABASE (ORACLE WALLET SUPPORT) ---
    implementation("com.oracle.database.jdbc:ojdbc17")
    implementation("com.oracle.database.security:oraclepki")
    implementation("com.oracle.database.security:osdt_cert:21.20.0.0")
    implementation("com.oracle.database.security:osdt_core:21.20.0.0")

    // --- LOMBOK & JACKSON ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-hibernate7")

    // --- TESTES ---
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")

    // Banco H2 em memória para testes de integração (Repository)
    runtimeOnly("com.h2database:h2")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.test {
    outputs.dir(project.extra["snippetsDir"]!!)
    finalizedBy(tasks.jacocoTestReport)
}

tasks.asciidoctor {
    inputs.dir(project.extra["snippetsDir"]!!)
    dependsOn(tasks.test)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
