package readme

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@UntrackedTask(because = "Opération Git — toujours exécutée en CI")
abstract class CommitGeneratedReadmeTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repoDir: DirectoryProperty

    @get:Input
    abstract val gitUserName: Property<String>

    @get:Input
    abstract val gitUserEmail: Property<String>

    @get:Input
    abstract val commitMessage: Property<String>

    @get:Input
    abstract val gitToken: Property<String>

    @TaskAction
    fun commitAndPush() {
        val root  = repoDir.get().asFile
        val token = gitToken.get()
        val credentials = UsernamePasswordCredentialsProvider("x-access-token", token)

        Git.open(root).use { git ->
            val status = git.status().call()

            if (status.isClean) {
                logger.lifecycle("Aucun fichier généré à commiter — dépôt propre.")
                return
            }

            val changed = status.added + status.changed + status.modified + status.untracked
            logger.lifecycle("Fichiers à commiter (${changed.size}) :")
            changed.forEach { logger.lifecycle("  + $it") }

            git.add().apply {
                addFilepattern("README*.adoc")
                addFilepattern(".github/workflows/readmes/images/")
            }.call()

            git.commit().apply {
                message = commitMessage.get()
                setAuthor(gitUserName.get(), gitUserEmail.get())
                setCommitter(gitUserName.get(), gitUserEmail.get())
            }.call()

            logger.lifecycle("Commit : \"${commitMessage.get()}\"")

            // ← setCredentialsProvider() méthode publique, pas accès direct au field
            git.push()
                .setCredentialsProvider(credentials)
                .call()

            logger.lifecycle("Push effectué avec succès.")
        }
    }
}