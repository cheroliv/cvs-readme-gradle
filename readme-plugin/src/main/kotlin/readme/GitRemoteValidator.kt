package readme

/**
 * SAM interface — validates remote git connectivity and permissions.
 * Production: JGitRemoteValidator
 * Test: lambda driven by -Preadme.git.validator.mock system property
 */
fun interface GitRemoteValidator {
    fun validate(config: GitConfig): GitValidationResult
}

sealed class GitValidationResult {
    object Valid                  : GitValidationResult()
    object TokenPlaceholder       : GitValidationResult()
    object Unreachable            : GitValidationResult()
    object RepositoryNotFound     : GitValidationResult()
    object InsufficientPushRights : GitValidationResult()
}
