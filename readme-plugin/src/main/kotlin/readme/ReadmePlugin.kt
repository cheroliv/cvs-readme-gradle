package readme

import org.gradle.api.Plugin
import org.gradle.api.Project

class ReadmePlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val config = ReadmePlantUmlConfig.load(project.projectDir)

        val processReadme = project.tasks.register(
            "processReadme",
            ProcessReadmeTask::class.java
        ) { task ->
            task.group       = "documentation"
            task.description = "Génère README*.adoc et .github/workflows/readmes/images/**/*.png depuis les sources _plantuml"

            task.sourceDir  .set(project.layout.projectDirectory.dir(config.source.dir))
            task.imgDir     .set(project.layout.projectDirectory.dir(config.output.imgDir))
            task.buildImgDir.set(project.layout.buildDirectory.dir("img"))
            task.defaultLang.set(config.source.defaultLang)
        }

        project.tasks.register(
            "commitGeneratedReadme",
            CommitGeneratedReadmeTask::class.java
        ) { task ->
            task.group       = "documentation"
            task.description = "Commite et pousse les README*.adoc générés via JGit (CI only)"

            task.repoDir      .set(project.layout.projectDirectory)
            task.gitUserName  .set(config.git.userName)
            task.gitUserEmail .set(config.git.userEmail)
            task.commitMessage.set(config.git.commitMessage)

            // ← lazy : résolu uniquement à l'exécution de commitGeneratedReadme
            //   pas au chargement du projet — processReadme reste utilisable
            //   localement sans token dans readme-plantuml.yml
            task.gitToken.set(project.provider { config.git.resolvedToken() })

            task.dependsOn(processReadme)
        }
    }
}