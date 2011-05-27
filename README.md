# Nexus

Nexus is a framework for the development of distributed applications
(including, but not limited to, massively multiplayer online games).

It is implemented in the Java language, though it is designed to be usable with
JavaScript-based clients via the Google Web Toolkit Java to JavaScript
compiler.

It is currently extremely unfinished.

## Building

At the top-level run:

    ant maven-deploy

to build the various jar files and install them into your local Maven
repository.

## Running the Chat Demo

There is a simple chat demo app. You can run it like so:

    cd demos/chat
    ant server

You can run the JVM client like so:

    cd demos/chat
    ant client

You can compile the JavaScript client thusly:

    cd demos/chat
    ant gclient

and you will then need to serve the contents of `demos/chat/dist/webapp` from
some web server. You will then be able to run the chat demo app from any
WebSockets enabled browser (including iOS and Android browsers).

You can also run the JavaScript client directly from GWT devmode, like so:

    cd demos/chat
    ant devmode

You will need the GWT devmode plugin installed in whatever browser you're
using.
