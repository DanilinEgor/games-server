package org.kuxurum.models

import kotlinx.serialization.Serializable

val tasksStorage = mutableListOf(
    Task("task1", "description1", Priority.Low),
    Task("task2", "description2", Priority.High),
)

enum class Priority {
    Low, Medium, High, Vital
}

@Serializable
data class Task(
    val name: String,
    val description: String,
    val priority: Priority
)

fun Task.taskAsRow(): String {
    return """
        <tr><td>$name</td><td>$description</td><td>$priority</td></tr>
    """.trimIndent()
}

fun List<Task>.asTable(): String {
    return joinToString(
        separator = "\n",
        prefix = "<table rules=\"all\">",
        postfix = "</table>",
        transform = { it.taskAsRow() }
    )
}