package tasks

import contributors.*
import kotlinx.coroutines.*

suspend fun loadContributorsConcurrent(service: GitHubService, req: RequestData): List<User> = coroutineScope {
    async {
        service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .body() ?: emptyList()
    }.await().map { repo ->
        async {
            service
                .getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }.awaitAll()
        .flatten()
        .aggregate()
}