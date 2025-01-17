import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    id("org.springframework.boot") version "{version}"
}

apply(plugin = "io.spring.dependency-management")

the<DependencyManagementExtension>().apply {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.springframework.boot") {
                useVersion("{version}")
            }
        }
    }
}

// tag::custom-version[]
extra["slf4j.version"] = "1.7.20"
// end::custom-version[]

repositories {
    mavenLocal()
}

task("slf4jVersion") {
    doLast {
        println(project.the<DependencyManagementExtension>().managedVersions["org.slf4j:slf4j-api"])
    }
}
