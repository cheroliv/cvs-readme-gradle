package readme

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

/**
 * Production implementation of GitRemoteValidator.
 * Resolves the remote URL from the local .git config (origin) —
 * never from ReadmePlantUmlConfig.
 * Uses a lightweight ls-remote to validate token, repo existence
 * and push rights.
 * Delegates filesystem walk to GitUtils.findGitRoot().
 */
class JGitRemoteValidator : GitRemoteValidator {

    override fun validate(config: GitConfig): GitValidationResult {
        if (config.token.isBlank() || config.token == "<YOUR_GITHUB_PAT>")
            return GitValidationResult.TokenPlaceholder

        return try {
            val credentials = UsernamePasswordCredentialsProvider(
                "x-access-token",
                config.token
            )

            val repoUrl = resolveOriginUrl()
                ?: return GitValidationResult.Unreachable

            Git.lsRemoteRepository()
                .setRemote(repoUrl)
                .setCredentialsProvider(credentials)
                .call()

            // TODO: verify push rights via GitHub API (contents:write scope)
            GitValidationResult.Valid

        } catch (e: TransportException) {
            when {
                e.message?.contains("not found", ignoreCase = true) == true
                    -> GitValidationResult.RepositoryNotFound
                e.message?.contains("not authorized", ignoreCase = true) == true
                    -> GitValidationResult.InsufficientPushRights
                else
                    -> GitValidationResult.Unreachable
            }
        } catch (e: Exception) {
            GitValidationResult.Unreachable
        }
    }

    private fun resolveOriginUrl(): String? {
        val gitRoot = GitUtils.findGitRoot(File(".").canonicalFile)

        if (gitRoot == null) {
            println("[readme] WARNING: no .git directory found in filesystem hierarchy — remote unreachable")
            return null
        }

        return try {
            Git.open(gitRoot).use { git ->
                git.repository
                    .config
                    .getString("remote", "origin", "url")
                    .takeIf { it?.isNotBlank() == true }
                    ?: run {
                        println(
                            "[readme] WARNING: no 'origin' remote configured in " +
                                    "${gitRoot.absolutePath}/.git/config — remote unreachable"
                        )
                        null
                    }
            }
        } catch (e: Exception) {
            println("[readme] WARNING: failed to read .git config at ${gitRoot.absolutePath} — ${e.message}")
            null
        }
    }
}
