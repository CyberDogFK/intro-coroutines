package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CountDownLatch

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .body() ?: emptyList()

        val channel = Channel<List<User>>()
        var usersList = listOf<User>()

        val countDownLatch = CountDownLatch(repos.size)
        repos.mapIndexed { index, repo ->
            async {
                log("starting loading for ${repo.name}")
                delay(3000)
                val users = service
                    .getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList()
                countDownLatch.countDown()
                channel.send(users)
            }.apply {
                launch {
                    log("enter into channel receiver____________________________________________________")
                    val receivedUsers = channel.receive()
                    usersList = (usersList + receivedUsers).aggregate()
                    updateResults(usersList, countDownLatch.count == 0L)
                }
            }
        }.awaitAll()
    }
}

fun main() = runBlocking<Unit> {
    val channel = Channel<String>()
    launch {
        channel.send("A1")
        channel.send("A2")
        log("A done")
    }
    launch {
        channel.send("B1")
        log("B done")
    }
    launch {
        repeat(3) {
            val x = channel.receive()
            log(x)
        }
    }
}
