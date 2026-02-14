import java.io.File



fun loadBooksFromCsv(path: String): List<Book> {
    val lines = File(path).readLines()

    return lines.drop(1).mapNotNull { line ->
        val parts = line.split(",")

        if (parts.size >= 2) {
            val title = parts[0].trim()
            val available = parts[1].trim().equals("available", ignoreCase = true)
            Book(title, available)
        } else null
    }
}
