package org.kuxurum.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.kuxurum.models.Article
import org.kuxurum.models.Articles

object DatabaseSingleton {
    fun init() {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:file:./build/db"
        val database = Database.connect(jdbcURL, driverClassName)
        transaction(database) {
            SchemaUtils.create(Articles)
        }
    }

    suspend fun <T> dbQuery(
        statement: suspend Transaction.() -> T
    ): T {
        return newSuspendedTransaction(Dispatchers.IO, statement = statement)
    }
}

interface DaoFacade {
    suspend fun allArticles(): List<Article>
    suspend fun article(id: Int): Article?
}

class DaoFacadeImpl : DaoFacade {
    override suspend fun allArticles(): List<Article> {
        return DatabaseSingleton.dbQuery {
            Articles.selectAll().map { row ->
                row.mapToArticle()
            }
        }
    }

    private fun ResultRow.mapToArticle() = Article(
        id = this[Articles.id],
        title = this[Articles.title],
        body = this[Articles.body],
    )

    override suspend fun article(id: Int): Article? {
        return DatabaseSingleton.dbQuery {
            Articles.select {
                Articles.id eq id
            }.map {
                it.mapToArticle()
            }.singleOrNull()
        }
    }
}