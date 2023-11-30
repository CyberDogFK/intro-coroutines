package tasks

import contributors.*
import java.util.concurrent.CountDownLatch

suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .body() ?: emptyList()

    var aggregatedList = listOf<User>()
    repos.forEachIndexed { index, repo ->
        log("starting loading for ${repo.name}")
        val res = service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
        aggregatedList = (aggregatedList + res).aggregate()
        updateResults(aggregatedList, index == repos.lastIndex)
    }
}
