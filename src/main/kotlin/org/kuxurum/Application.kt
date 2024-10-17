package org.kuxurum

import freemarker.cache.ClassTemplateLoader
import freemarker.core.HTMLOutputFormat
import freemarker.core.OutputFormat
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.freemarker.FreeMarker
import org.kuxurum.data.DatabaseSingleton
import org.kuxurum.plugins.configureRouting
import org.kuxurum.plugins.configureSerialization
import org.kuxurum.plugins.configureWebSocket

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    DatabaseSingleton.init()
    configureSerialization()
    configureRouting()
    configureWebSocket()
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        outputFormat = HTMLOutputFormat.INSTANCE
    }
}
