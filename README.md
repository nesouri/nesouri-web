# Nesouri web frontend

The purpose of this application is to add an editing frontend to the database, and to learn Clojure/ClojureScript.

## Running

To start a web server for the application, run:

    lein figwheel

## Technology

### Server side

Implemented in Clojure, REST resources shared via compojure/ring, data gathered via korma SQL DSL.

### Client side

Implemented in ClojureScript based on React.js wrapper Om. To remove boilerplate from components om-tools is used. Cross-component communication is handled via core.async, CSS stuff via om-bootstrap, and sub-page routing via secretary.

## License

Copyright © 2015 Daniel Svensson.

All files licensed under AGPLv3 (see LICENSE).
