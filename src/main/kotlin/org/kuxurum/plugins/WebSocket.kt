package org.kuxurum.plugins

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.suitableCharset
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.kuxurum.models.Priority
import org.kuxurum.models.Task
import java.time.Duration

fun Application.configureWebSocket() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json {
            serializersModule = SerializersModule {
                polymorphic(Message::class) {
                    subclass(RegisterWs::class)
                    subclass(GameCreated::class)
                    subclass(FoundSecondPlayer::class)
                }
            }
        })

        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/tasks") {
            val tasks = listOf(
                Task("cleaning", "Clean the house", Priority.Low),
                Task("gardening", "Mow the lawn", Priority.Medium),
                Task("shopping", "Buy the groceries", Priority.High),
                Task("painting", "Paint the fence", Priority.Medium)
            )

            tasks.forEach {
                sendSerialized(it)
                delay(1000L)
            }

            close(CloseReason(CloseReason.Codes.NORMAL, "All done"))
        }

        webSocket("/connect") {
            incoming.consumeEach { frame ->
                val message = converter?.deserialize(
                    call.request.headers.suitableCharset(),
                    typeInfo<Message>(),
                    frame
                )

                println("message=$message")

                when (message) {
                    is RegisterWs -> {
                        sessions[message.id] = this
                    }
                }
            }
        }
    }
}

val sessions = mutableMapOf<String, DefaultWebSocketServerSession>()

@Serializable
sealed class Message

@Serializable
@SerialName("register")
data class RegisterWs(val id: String) : Message()

@Serializable
@SerialName("game_created")
data class GameCreated(val id: String) : Message()

@Serializable
@SerialName("found_second_player")
data class FoundSecondPlayer(val id: String) : Message()

@Serializable
@SerialName("turn_made")
class TurnMade(val field: Array<IntArray>) : Message()

@Serializable
@SerialName("game_ended")
data class GameEnded(val playerWon: String) : Message()
