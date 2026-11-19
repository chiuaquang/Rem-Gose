// mavenLocal() dat TRUOC de moi truong build offline (may cua Rem) dung duoc
// kho thu vien tai san trong ~/.m2; tren may binh thuong Gradle se bo qua
// cac artifact khong co o local va tai tu google()/mavenCentral() nhu thuong.
pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
rootProject.name = "RemGose"
include(":app")
