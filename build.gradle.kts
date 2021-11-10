plugins {
   java
}

group = "net.leanix"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.2")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:1.7.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("org.slf4j:slf4j-simple:1.7.32")
}

tasks.test {
    useJUnitPlatform()
}