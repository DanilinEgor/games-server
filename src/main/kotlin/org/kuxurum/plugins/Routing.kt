package org.kuxurum.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import io.ktor.server.websocket.sendSerialized
import kotlinx.serialization.Serializable
import org.kuxurum.models.Article
import org.kuxurum.models.Priority
import org.kuxurum.models.articles
import org.kuxurum.models.asTable
import org.kuxurum.models.tasksStorage
import org.kuxurum.routes.customerRouting
import org.kuxurum.routes.orderRouting
import java.util.UUID

fun Application.configureRouting() {
    routing {
        staticResources("/static", "files")
        customerRouting()
        orderRouting()

        route("articles") {
            get {
                call.respond(FreeMarkerContent("index.ftl", mapOf("articles" to articles)))
            }

            get("new") {
                call.respond(FreeMarkerContent("new.ftl", model = null))
            }

            post {
                val parameters = call.receiveParameters()
                val title = parameters.getOrFail("title")
                val body = parameters.getOrFail("body")
                val newEntry = Article.newEntry(title, body)
                articles.add(newEntry)
                call.respondRedirect("/articles/${newEntry.id}")
            }

            get("{id}") {
                val id = call.parameters.getOrFail<Int>("id").toInt()
                call.respond(
                    FreeMarkerContent(
                        "show.ftl",
                        mapOf("article" to articles.find { it.id == id })
                    )
                )
            }

            get("{id}/edit") {
                val id = call.parameters.getOrFail<Int>("id").toInt()
                call.respond(
                    FreeMarkerContent(
                        "edit.ftl",
                        mapOf("article" to articles.find { it.id == id })
                    )
                )
            }

            post("{id}") {
                val id = call.parameters.getOrFail("id").toInt()
                val formParameters = call.receiveParameters()
                when (formParameters.getOrFail("_action")) {
                    "update" -> {
                        val article = articles.find { it.id == id } ?: return@post
                        article.title = formParameters.getOrFail("title")
                        article.body = formParameters.getOrFail("body")
                        call.respondRedirect("/articles/${id}")
                    }

                    "delete" -> {
                        articles.removeIf { it.id == id }
                        call.respondRedirect("/articles")
                    }
                }
            }
        }

        get("/tasks") {
            call.respondText(
                contentType = ContentType.parse("text/html"),
                text = tasksStorage.asTable(),
            )
        }

        get("/tasks/byPriority/{priority}") {
            val priorityText = call.parameters["priority"] ?: return@get call.respondText(
                "No priority found",
                status = HttpStatusCode.BadRequest
            )
            val priority = try {
                Priority.valueOf(priorityText)
            } catch (e: Exception) {
                return@get call.respondText(
                    "No priority found",
                    status = HttpStatusCode.BadRequest
                )
            }

            val tasks = tasksStorage.filter { it.priority == priority }
            if (tasks.isEmpty()) {
                call.respondText(status = HttpStatusCode.NotFound, text = "No tasks")
            } else {
                call.respondText(
                    contentType = ContentType.parse("text/html"),
                    text = tasks.asTable()
                )
            }
        }

        post("/createGame") {
            val params = call.receiveParameters()
            val id = params.getOrFail("id")
            val name = params.getOrFail("name")
            val gameId = freeGamesIds.firstOrNull() ?: run {
                val uuid = UUID.randomUUID().toString()
                freeGamesIds.add(uuid)
                uuid
            }
            val newGame = games[gameId]?.let {
                it.status as Game.Status.WaitingForSecondPlayer
                sessions[it.status.id]?.sendSerialized(FoundSecondPlayer(id) as Message)
                actualGames[gameId] = ActualGame(Array(3) { IntArray(3) { 0 } }, it.status.id, id)
                Game(gameId = gameId, status = Game.Status.InProgress(it.status.id, it.status.name))
            } ?: run {
                sessions[id]?.sendSerialized(GameCreated(id) as Message)
                Game(gameId = gameId, status = Game.Status.WaitingForSecondPlayer(id, name))
            }

            call.respond(newGame)
            games[gameId] = newGame
        }

        get("/getGameStatus/{id}") {
            val id = call.parameters.getOrFail("id")
            games[id]?.let {
                call.respond(it)
            } ?: run {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post("/makeTurn") {
            val params = call.receiveParameters()
            val gameId = params.getOrFail("gameId")
            val playerId = params.getOrFail("playerId")
            val turnX = params.getOrFail("turnX").toInt()
            val turnY = params.getOrFail("turnY").toInt()
            val game = actualGames[gameId] ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val num = when (playerId) {
                game.player1Id -> 1
                game.player2Id -> 2
                else -> run {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }
            }

            game.field[turnX][turnY] = num

            val event = TurnMade(game.field)
            sessions[game.player1Id]?.sendSerialized(event)
            sessions[game.player2Id]?.sendSerialized(event)

            isGameEnded(game)?.let {
                val playerWonId = when (it) {
                    1 -> game.player1Id
                    2 -> game.player2Id
                    else -> return@post
                }
                val event = GameEnded(
                    playerWon = playerWonId
                )
                sessions[game.player1Id]?.sendSerialized(event)
                sessions[game.player2Id]?.sendSerialized(event)
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
data class Game(
    val gameId: String,
    val status: Status,
) {
    @Serializable
    sealed class Status {
        @Serializable
        data class WaitingForSecondPlayer(val id: String, val name: String) : Status()

        @Serializable
        data class InProgress(val id: String, val name: String) : Status()
    }
}

@Serializable
data class ActualGame(
    val field: Array<IntArray>,
    val player1Id: String,
    val player2Id: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActualGame

        return field.contentDeepEquals(other.field)
    }

    override fun hashCode(): Int {
        return field.contentDeepHashCode()
    }
}

val games = mutableMapOf<String, Game>()
val actualGames = mutableMapOf<String, ActualGame>()
val freeGamesIds = mutableListOf<String>()

fun isGameEnded(game: ActualGame): Int? {
    for (i in 0..2) {
        var hasWin = true
        val check = game.field[i][0]
        for (j in 1..2) {
            if (game.field[i][j] != check) {
                hasWin = false
            }
        }
        if (hasWin) return check
    }

    for (i in 0..2) {
        var hasWin = true
        val check = game.field[0][i]
        for (j in 1..2) {
            if (game.field[j][i] != check) {
                hasWin = false
            }
        }
        if (hasWin) return check
    }

    run {
        var hasWin = true
        val check = game.field[0][0]
        for (i in 1..2) {
            if (game.field[i][i] != check) {
                hasWin = false
            }
            if (hasWin) return check
        }
    }

    run {
        var hasWin = true
        val check = game.field[2][0]
        for (i in 1..2) {
            if (game.field[2 - i][i] != check) {
                hasWin = false
            }
            if (hasWin) return check
        }
    }

    return null
}