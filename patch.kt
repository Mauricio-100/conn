import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    languageVersion.set(KotlinVersion.KOTLIN_2_1)
    apiVersion.set(KotlinVersion.KOTLIN_2_1)
  }
}
