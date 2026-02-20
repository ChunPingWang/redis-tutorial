plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "redis-tutorial"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("common")
include("module-01-getting-started")
include("module-02-data-structures")
include("module-03-specialized-structures")
include("module-04-caching-patterns")
include("module-05-pipelining-transactions")
include("module-06-data-modeling")
include("module-07-streams-events")
include("module-08-persistence")
include("module-09-high-availability")
include("module-10-clustering")
include("module-11-search-indexing")
include("module-12-json-vector")
include("module-13-security-production")
include("module-14-capstone")
