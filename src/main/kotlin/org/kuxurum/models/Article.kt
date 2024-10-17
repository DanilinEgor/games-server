package org.kuxurum.models

import org.jetbrains.exposed.sql.Table
import java.util.concurrent.atomic.AtomicInteger

class Article constructor(
    val id: Int,
    var title: String,
    var body: String,
) {
    companion object {
        private val idInteger = AtomicInteger()
        fun newEntry(
            title: String,
            body: String,
        ) = Article(
            id = idInteger.getAndIncrement(),
            title = title,
            body = body,
        )
    }
}

val articles = mutableListOf(
    Article.newEntry(
        "The drive to develop!",
        "...it's what keeps me going."
    )
)

object Articles : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 128)
    val body = varchar("body", 1024)

    override val primaryKey = PrimaryKey(id)
}