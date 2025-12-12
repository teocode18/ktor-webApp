# Music Club Demo Application

Web application for a music club, implemented using [Ktor][ktr], [htmx][hx],
[Pebble][peb] templates and the [Exposed][orm] ORM framework. Also included
is a small application that creates the music club's database, and another
that demonstrates querying that database using Exposed's SQL DSL.

## Running The Demo

Everything is managed by [Amper][amp]. Start by creating the database
with

    ./amper run -m create

You will see the SQL statements echoed in the terminal. The database is
created in the file `music.db`.

To test whether the database can be queried successfully, do

    ./amper run -m query

You will see some SQL queries and their results echoed in the terminal.

Run the server with

    ./amper run -m server

then visit `http://0.0.0.0:8080/` to interact with the application.

## Examining The Code

### Database

Amper module `database` contains all of the code needed to connect to the
databases used by the application, create the required database tables, and
interact with those tables. This library of code is used by the query demo
and the server.

`Tables.kt` defines the database schema.

`Entities.kt` implements the object-relational mapping, providing entity
classes for the tables defined in `Tables.kt`.

`Database.kt` defines functions for connecting to and creating the databases
used by the web application.

### Query Demo

Amper module `query` contains a program in `Main.kt` that demonstrates
the use of Exposed's SQL DSL to perform simple queries of the music club
database.

### Server

Amper module `server` contains a Ktor-based web application.

`Templates.kt` contains the configuration needed to support the use of
Pebble templates.

The bulk of the application logic is in `Routing.kt`. This sets up the
routing of GET and POST requests to the code that can handle the request.
That code uses Exposed's Data Access Objects API to make the necessary
queries, then uses query results to render the appropriate template into
HTML. The template files can be found in `resources/templates`.

`Application.kt` provides the entry point for the application. It delegates
configuration to the extension functions defined in the aforementioned files,
and specifies a server to run the application.

[ktr]: https://ktor.io/
[hx]: https://htmx.org/
[peb]: https://pebbletemplates.io/
[orm]: https://jetbrains.github.io/Exposed/
[amp]: https://amper.org/
