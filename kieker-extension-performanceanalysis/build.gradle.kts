plugins {
    application
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")

    // logging
    implementation ("ch.qos.logback:logback-classic:1.2.9")
    implementation ("org.slf4j:slf4j-api:1.7.30")

    // cli - https://jcommander.org/
    implementation ("com.beust", "jcommander", "1.82")

    // emf / ecore / Uml2
    implementation(fileTree("../libs"))

    // epsilon
    implementation("org.eclipse.epsilon:org.eclipse.epsilon.emc.emf:2.4.0")
    implementation("org.eclipse.epsilon:org.eclipse.epsilon.eol.engine:2.4.0")
    implementation("org.eclipse.epsilon:org.eclipse.epsilon.etl.engine:2.4.0")
    implementation("org.eclipse.epsilon:org.eclipse.epsilon.egl.engine:2.4.0")
    implementation("org.eclipse.epsilon:org.eclipse.epsilon.evl.engine:2.4.0")
    implementation("org.eclipse.epsilon:org.eclipse.epsilon.emc.plainxml:2.4.0")

    // kieker
    implementation ("net.kieker-monitoring:kieker:2.0.0-SNAPSHOT")
    implementation ("de.cau.cs.se.teetime:teetime:3.1.0")
}

application {
    mainClass.set("kieker.extension.performanceanalysis.Main")
}

java {
    // AspectJ and Epsilon uses reflections which are no longer allowed after Java version 11
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}