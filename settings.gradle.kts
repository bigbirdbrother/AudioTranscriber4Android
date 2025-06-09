pluginManagement {
    repositories {
        google() // 这里配置 Google 仓库
        mavenCentral() // 配置 Maven Central 仓库
        maven { url = uri("https://maven.aliyun.com/repository/google") } // 配置阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") } // 配置阿里云公共镜像
        jcenter() // 如果需要的话
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // 确保使用 settings 中的仓库
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        jcenter()
    }
}

rootProject.name = "AudioTranscriber4Android"
include(":app")
