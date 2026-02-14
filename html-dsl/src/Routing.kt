import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.html.*
import java.io.BufferedReader
import java.io.InputStreamReader

// ---------- Resource helpers ----------
private fun readResourceLines(path: String): List<String> {
    val stream = object {}.javaClass.getResourceAsStream(path) ?: return emptyList()
    return BufferedReader(InputStreamReader(stream)).readLines()
}

// Basic CSV splitter (handles quoted commas)
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
 * title,author,isbn_13,format_code,location_code,notes
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

    val titles = lines.drop(1).mapNotNull { line ->
        val cols = splitCsvLine(line)
        if (cols.size <= titleIdx) null else cols[titleIdx].trim().takeIf { it.isNotBlank() }
    }.distinct()

    return titles.map { title ->
        val isOnLoan = loanedSet.contains(title)
        Book(title = title, available = !isOnLoan)
    }
}

// ---------- Shared CSS + UI ----------
private fun sharedCss(): String = """
    body { font-family: Arial, sans-serif; margin: 0; background: #F2F2F2; color: #1A1A1A; }
    .wrap { max-width: 900px; margin: 40px auto; padding: 0 16px; }
    .card { border: 2px solid #1A1A1A; padding: 24px; background: #FFFFFF; }
    h1 { font-size: 40px; margin: 0 0 16px 0; }
    h2 { font-size: 34px; margin: 18px 0 8px 0; }
    label { font-weight: 700; display: block; margin: 12px 0 8px; font-size: 22px; }
    .searchRow { display: flex; gap: 16px; align-items: center; }
    input[type="text"] { flex: 1; font-size: 22px; padding: 14px; border: 2px solid #1A1A1A; background: #FFFFFF; }
    button { font-size: 22px; padding: 14px 22px; border: 2px solid #1A1A1A; background: #FFFFFF; cursor: pointer; }
    button:focus, input:focus { outline: 3px solid #1A1A1A; outline-offset: 2px; }
    .results { margin-top: 18px; }
    .resultItem { display: flex; justify-content: space-between; padding: 14px; border: 2px solid #1A1A1A; background: #FFFFFF; margin-top: 12px; font-size: 20px; }
    .status { font-weight: 800; }
    .available { color: #006400; }
    .onloan { color: #A04000; }
    .msg { margin-top: 14px; font-weight: 800; font-size: 20px; }
""".trimIndent()

private fun FlowContent.resultRow(title: String, statusText: String, isAvailable: Boolean) {
    div("resultItem") {
        span { +title }
        span(classes = "status " + if (isAvailable) "available" else "onloan") { +statusText }
    }
}

private fun HTML.renderPage(
    pageTitle: String,
    sectionHeading: String,
    books: List<Book>,
    query: String,
    results: List<Book>,
    message: String?
) {
    head {
        title(pageTitle)
        style { unsafe { +sharedCss() } }
    }
    body {
        div("wrap") {
            h1 { +"Search for Book" }

            div("card") {

                // Search bar shown on BOTH pages
                form(action = "/search", method = FormMethod.get) {
                    label {
                        attributes["for"] = "title"
                        +"Search for Book"
                    }
                    div("searchRow") {
                        textInput(name = "title") {
                            id = "title"
                            value = query
                            placeholder = "Enter book title..."
                        }
                        button(type = ButtonType.submit) { +"Search" }
                    }
                }

                div("results") {
                    h2 { +sectionHeading }

                    when {
                        books.isEmpty() ->
                            p("msg") { +"No books loaded. Check resources/LibraryBookList.csv and resources/loaned_titles.txt." }

                        message != null ->
                            p("msg") { +message }

                        else ->
                            results.forEach { b ->
                                resultRow(b.title, if (b.available) "Available" else "On loan", b.available)
                            }
                    }
                }

                br
                a("/") { +"Back to search" }
            }
        }
    }
}

// ---------- Ktor routing ----------
fun Application.configureRouting() {

    // CSV + loaned titles
    val books = loadBooksFromCopiesCsv(
        csvResource = "/LibraryBookList.csv",
        loanedTitlesResource = "/loaned_titles.txt"
    )

    routing {

        // Home page: show first few books
        get("/") {
            call.respondHtml {
                val preview = books.take(5)
                renderPage(
                    pageTitle = "Library Search",
                    sectionHeading = "Library Highlights",
                    books = books,
                    query = "",
                    results = preview,
                    message = null
                )
            }
        }

        // Search page: styled the same as home page
        get("/search") {
            val query = call.request.queryParameters["title"]?.trim().orEmpty()

            val results =
                if (query.isBlank()) emptyList()
                else books.filter { it.title.contains(query, ignoreCase = true) }

            val message = when {
                books.isEmpty() -> null // handled inside renderPage
                query.isBlank() -> "Please enter a title."
                results.isEmpty() -> "No books found."
                else -> null
            }

            call.respondHtml {
                renderPage(
                    pageTitle = "Search Results",
                    sectionHeading = "Results",
                    books = books,
                    query = query,
                    results = results,
                    message = message
                )
            }
        }
    }
}
