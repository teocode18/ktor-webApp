import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.html.*
import java.io.BufferedReader
import java.io.InputStreamReader

private fun readResourceLines(path: String): List<String> {
    val stream = object {}.javaClass.getResourceAsStream(path) ?: return emptyList()
    return BufferedReader(InputStreamReader(stream)).readLines()
}

private fun splitCsvLine(line: String): List<String> {
    val out = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false

    for (ch in line) {
        when (ch) {
            '"' -> inQuotes = !inQuotes
            ',' -> if (inQuotes) sb.append(ch) else { out.add(sb.toString()); sb.setLength(0) }
            else -> sb.append(ch)
        }
    }
    out.add(sb.toString())
    return out
}

/**
 * Your CSV is a "copies" list:
 * title, author, isbn_13, format_code, location_code, notes
 *
 * We convert it into unique titles and mark availability using loaned_titles.txt.
 */
private fun loadBooksFromCopiesCsv(
    csvResource: String,
    loanedTitlesResource: String
): List<Book> {

    val lines = readResourceLines(csvResource)
    if (lines.isEmpty()) return emptyList()

    val header = splitCsvLine(lines.first()).map { it.trim().lowercase() }
    val titleIdx = header.indexOf("title")
    if (titleIdx == -1) return emptyList()

    val loanedSet = readResourceLines(loanedTitlesResource)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()

    // Collect unique titles
    val titles = lines.drop(1).mapNotNull { line ->
        val cols = splitCsvLine(line)
        if (cols.size <= titleIdx) null else cols[titleIdx].trim().takeIf { it.isNotBlank() }
    }.distinct()

    return titles.map { title ->
        val isOnLoan = loanedSet.contains(title)
        Book(title = title, available = !isOnLoan)
    }
}

fun Application.configureRouting() {

    val books = loadBooksFromCopiesCsv(
        csvResource = "/LibraryBookList.csv",
        loanedTitlesResource = "/loaned_titles.txt"
    )

    routing {

        get("/") {
            call.respondHtml {
                head {
                    title("Library Search")
                    style {
                        unsafe {
                            +"""
                            body { font-family: Arial, sans-serif; margin: 0; background: #FFFFFF; color: #1A1A1A; }
                            .wrap { max-width: 900px; margin: 40px auto; padding: 0 16px; }
                            .card { border: 2px solid #1A1A1A; padding: 24px; background: #F2F2F2; }
                            h1 { font-size: 28px; margin: 0 0 12px 0; }
                            label { font-weight: 700; display: block; margin: 12px 0 8px; }
                            .searchRow { display: flex; gap: 12px; align-items: center; }
                            input[type="text"] { flex: 1; font-size: 18px; padding: 12px; border: 2px solid #1A1A1A; background: #FFFFFF; }
                            button { font-size: 18px; padding: 12px 18px; border: 2px solid #1A1A1A; background: #FFFFFF; cursor: pointer; }
                            button:focus, input:focus { outline: 3px solid #1A1A1A; outline-offset: 2px; }
                            .results { margin-top: 18px; }
                            .resultItem { display: flex; justify-content: space-between; padding: 12px; border: 2px solid #1A1A1A; background: #FFFFFF; margin-top: 10px; }
                            .status { font-weight: 700; }
                            .available { color: #2E7D32; }
                            .onloan { color: #C45A00; }
                            .msg { margin-top: 14px; font-weight: 700; }
                            """.trimIndent()
                        }
                    }
                }
                body {
                    div("wrap") {
                        h1 { +"Search for Book" }

                        div("card") {
                            form(action = "/search", method = FormMethod.get) {
                                label {
                                    attributes["for"] = "title"
                                    +"Search for Book"
                                }
                                div("searchRow") {
                                    textInput(name = "title") {
                                        id = "title"
                                        placeholder = "Enter book title..."
                                    }
                                    button(type = ButtonType.submit) { +"Search" }
                                }
                            }

                            if (books.isEmpty()) {
                                p("msg") { +"No books loaded. Check resources/LibraryBookList.csv and resources/loaned_titles.txt." }
                            } else {
                                div("results") {
                                    h2 { +"Results..." }
                                    books.take(5).forEach { b ->
                                        resultRow(b.title, if (b.available) "Available" else "On loan", b.available)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        get("/search") {
            val query = call.request.queryParameters["title"]?.trim().orEmpty()
            val results =
                if (query.isBlank()) emptyList()
                else books.filter { it.title.contains(query, ignoreCase = true) }

            call.respondHtml {
                head { title("Search Results") }
                body {
                    div("wrap") {
                        h1 { +"Results" }

                        div("card") {
                            p { +"Search term: \"$query\"" }

                            when {
                                books.isEmpty() -> p("msg") { +"No books loaded. Check resources files." }
                                query.isBlank() -> p("msg") { +"Please enter a title." }
                                results.isEmpty() -> p("msg") { +"No books found." }
                                else -> results.forEach { b ->
                                    resultRow(b.title, if (b.available) "Available" else "On loan", b.available)
                                }
                            }

                            br
                            a("/") { +"Back to search" }
                        }
                    }
                }
            }
        }
    }
}

private fun FlowContent.resultRow(title: String, statusText: String, isAvailable: Boolean) {
    div("resultItem") {
        span { +title }
        span(classes = "status " + if (isAvailable) "available" else "onloan") { +statusText }
    }
}
