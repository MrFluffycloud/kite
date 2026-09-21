pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Kite"
include(":app")
include(":core:model")
include(":core:database")
include(":core:domain")
include(":core:data")
include(":feature:home")
include(":feature:transactions")
include(":feature:categories")
include(":feature:accounts")
include(":feature:settings")
include(":feature:insights")
include(":feature:debts")
include(":feature:vault")
include(":feature:recurring")
include(":platform:security")
include(":platform:widget")
include(":platform:notification")
