import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.html.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLEncoder

// ---------- Resource helpers ----------
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
            ',' -> if (inQuotes) sb.append(ch) else {
                out.add(sb.toString())
                sb.setLength(0)
            }
            else -> sb.append(ch)
        }
    }
    out.add(sb.toString())
    return out
}

private fun loadTitlesFromCopiesCsv(csvResource: String): List<String> {
    val lines = readResourceLines(csvResource)
    if (lines.isEmpty()) return emptyList()

    val header = splitCsvLine(lines.first()).map { it.trim().lowercase() }
    val titleIdx = header.indexOf("title")
    if (titleIdx == -1) return emptyList()

    return lines.drop(1).mapNotNull { line ->
        val cols = splitCsvLine(line)
        if (cols.size <= titleIdx) null
        else cols[titleIdx].trim().takeIf { it.isNotBlank() }
    }.distinct()
}

// ---------- HTML escape helper (for autocomplete) ----------
private fun htmlEscape(s: String): String =
    s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

// ---------- Shared CSS ----------
private fun sharedCss(): String = """
    body { font-family: Arial, sans-serif; margin: 0; background: #F2F2F2; color: #1A1A1A; }
    .wrap { max-width: 900px; margin: 40px auto; padding: 0 16px; }
    .card { border: 2px solid #1A1A1A; padding: 24px; background: #F2F2F2; }
    h1 { font-size: 40px; margin: 0 0 16px 0; }
    h2 { font-size: 34px; margin: 18px 0 8px 0; }
    label { font-weight: 700; display: block; margin: 12px 0 8px; font-size: 22px; }
    .searchRow { display: flex; gap: 16px; align-items: center; }
    input[type="text"] { flex: 1; font-size: 22px; padding: 14px; border: 2px solid #1A1A1A; background: #FFFFFF; }
    button { font-size: 22px; padding: 14px 22px; border: 2px solid #1A1A1A; background: #FFFFFF; cursor: pointer; }
    .results { margin-top: 18px; }
    .resultItem { display: flex; justify-content: space-between; padding: 14px; border: 2px solid #1A1A1A; background: #FFFFFF; margin-top: 12px; font-size: 20px; }
    .status { font-weight: 800; }
    .available { color: #2E7D32; }
    .onloan { color: #C45A00; }
    .msg { margin-top: 14px; font-weight: 800; font-size: 20px; }
""".trimIndent()

// ---------- Book row ----------
private fun FlowContent.resultRow(title: String, isAvailable: Boolean) {
    div("resultItem") {
        span { +title }

        div {
            span(classes = "status " + if (isAvailable) "available" else "onloan") {
                +(if (isAvailable) "Available" else "On loan")
            }

            if (isAvailable) {
                +" "
                form(action = "/borrow", method = FormMethod.post) {
                    style = "display:inline-block; margin-left: 12px;"
                    hiddenInput(name = "title") { value = title }
                    button(type = ButtonType.submit) { +"Borrow" }
                }
            }
        }
    }
}

// ---------- Page renderer ----------
private fun HTML.renderPage(
    pageTitle: String,
    sectionHeading: String,
    books: List<Book>,
    query: String,
    results: List<Book>,
    message: String?,
    returnMessage: String?
) {
    head {
        title(pageTitle)
        style { unsafe { +sharedCss() } }
    }
    body {
        div("wrap") {
            h1 { +"Search for Book" }

            div("card") {

                // Search form + autocomplete
                form(action = "/search", method = FormMethod.get) {
                    label { +"Search for Book" }

                    div("searchRow") {
                        textInput(name = "title") {
                            value = query
                            placeholder = "Enter book title..."
                            attributes["list"] = "titles"
                        }
                        button { +"Search" }
                    }

                    // Autocomplete list (raw HTML so it compiles)
                    unsafe {
                        +buildString {
                            append("""<datalist id="titles">""")
                            books.take(500).forEach { b ->
                                val t = htmlEscape(b.title)
                                append("""<option value="$t"></option>""")
                            }
                            append("</datalist>")
                        }
                    }
                }

                // Results
                div("results") {
                    h2 { +sectionHeading }

                    when {
                        message != null ->
                            p("msg") { +message }
                        else ->
                            results.forEach { b ->
                                resultRow(b.title, b.available)
                            }
                    }
                }

                // Return section
                div("results") {
                    h2 { +"Return a book" }

                    if (returnMessage != null) {
                        p("msg") { +returnMessage }
                    }

                    form(action = "/return", method = FormMethod.post) {
                        div("searchRow") {
                            textInput(name = "code") {
                                placeholder = "Enter request number (e.g. 001)"
                            }
                            button { +"Return" }
                        }
                    }
                }

                br
                a("/") { +"Back to search" }
            }
        }
    }
}

// ---------- Routing ----------
fun Application.configureRouting() {

    val titles = loadTitlesFromCopiesCsv("/LibraryBookList.csv")

    val loanedTitles = mutableSetOf<String>()
    val activeRequests = mutableMapOf<String, String>() // code -> title

    var requestCounter = 1

    fun currentBooks(): List<Book> =
        titles.map { t -> Book(title = t, available = !loanedTitles.contains(t)) }

    fun nextRequestCode(): String = "%03d".format(requestCounter++)

    routing {

        // Borrow: create code, mark on loan, redirect with code
        post("/borrow") {
            val params = call.receiveParameters()
            val title = params["title"]?.trim().orEmpty()

            val code = nextRequestCode()
            loanedTitles.add(title)
            activeRequests[code] = title

            val safe = URLEncoder.encode(title, "UTF-8")
            call.respondRedirect("/search?title=$safe&borrowed=$code")
        }

        // Return: must enter correct code. Redirect includes title for message.
        post("/return") {
            val params = call.receiveParameters()
            val code = params["code"]?.trim().orEmpty()

            val title = activeRequests[code]
            if (title != null) {
                loanedTitles.remove(title)
                activeRequests.remove(code)

                val safeTitle = URLEncoder.encode(title, "UTF-8")
                call.respondRedirect("/search?returned=$code&book=$safeTitle")
            } else {
                call.respondRedirect("/search?return_error=1")
            }
        }

        // Home page
        get("/") {
            val books = currentBooks()
            call.respondHtml {
                renderPage(
                    pageTitle = "Library Search",
                    sectionHeading = "Library Highlights",
                    books = books,
                    query = "",
                    results = books.take(5),
                    message = null,
                    returnMessage = null
                )
            }
        }

        // Search page
        get("/search") {
            val query = call.request.queryParameters["title"]?.trim().orEmpty()
            val borrowedCode = call.request.queryParameters["borrowed"]
            val returnedCode = call.request.queryParameters["returned"]
            val returnedBook = call.request.queryParameters["book"]
            val returnError = call.request.queryParameters["return_error"]

            val books = currentBooks()

            val results =
                if (query.isBlank()) emptyList()
                else books.filter { it.title.contains(query, ignoreCase = true) }

            val message = when {
                borrowedCode != null ->
                    "Please get your book at reception. Request number: $borrowedCode"
                results.isEmpty() && query.isNotBlank() ->
                    "No books found."
                else -> null
            }

            val returnMessage = when {
                returnedCode != null && returnedBook != null ->
                    "Return successful. \"$returnedBook\" has been returned. Code $returnedCode confirmed."
                returnError != null ->
                    "Invalid request number."
                else -> null
            }

            call.respondHtml {
                renderPage(
                    pageTitle = "Search Results",
                    sectionHeading = "Results",
                    books = books,
                    query = query,
                    results = results,
                    message = message,
                    returnMessage = returnMessage
                )
            }
        }
    }
}
