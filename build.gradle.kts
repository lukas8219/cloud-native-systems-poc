plugins {
    application
    id("java")
}

apply(plugin = "application")

application {
    mainClass.set("lukas8219.Main")
}

group = "org.example"
version = "1.0-SNAPSHOT"
repositories {
    mavenLocal()
    mavenCentral()
}


dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("com.automq.elasticstream:s3stream:1.2.0-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}