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

        maven {
            url= uri("https://jitpack.io")
        }
        maven(url = "https://storage.googleapis.com/download.flutter.io")
        maven {
           // url =uri("/Users/tanvir/Shimul Tamo/ekyc_Module_Repo_File/repo")
            url =uri("/Users/tanvir/Xcode Projects/smart_banking/MTB-Smart-Banking-Module/build/host/outputs/repo")
        }

    }
}

rootProject.name = "loanAppAndroid"
include(":app")
 