plugins {
    id("application") 
    id("java") 
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.micronaut:micronaut-bom:3.9.4"))
    implementation("io.micronaut.aws:micronaut-aws-cdk") {
      exclude(group = "software.amazon.awscdk", module = "aws-cdk-lib")
    }
    implementation("software.amazon.awscdk:aws-cdk-lib:2.77.0")
    testImplementation(platform("io.micronaut:micronaut-bom:3.9.4"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
}
application {
    mainClass.set("com.bytestream.Main")
}
tasks.withType<Test> {
    useJUnitPlatform()
}

