# Nexus

Nexus is a framework for the development of distributed applications (including, but not limited
to, massively multiplayer online games).

It is implemented in the Java language, but it is a cross-platform toolkit. Nexus clients can
currently be deployed on the following combinations of platform/technology:

  * Windows desktop: JVM, HTML5 web browser
  * MacOS desktop: JVM, HTML5 web browser
  * Linux desktop: JVM, HTML5 web browser
  * Android device: native app (Java/Dalvik), HTML5 web browser
  * iOS device: native app (via IKVM and MonoTouch), HTML5 web browser

Nexus servers are generally run via the JVM on any server platform that supports the JVM.

Nexus is currently functional for single server systems. Support for scaling a Nexus installation
to multiple servers is incomplete, but coming soon.

## Building

The Nexus libraries are published to [Maven Central] for easy integration into your build. You can
also build and install the latest SNAPSHOT version of Nexus thusly:

    mvn install

This will build the various library jar files and install them into your local Maven repository.

## Demos

Check out the [nexus-demos] project for demo code.

## Discuss

Questions and comments can be directed to the [OOO Google Group].

## License

Nexus is released under the BSD License. See the [LICENSE] file for details.

[nexus-demos]: https://github.com/threerings/nexus-demos
[OOO Google Group]: http://groups.google.com/group/ooo-libs
[LICENSE]: https://github.com/threerings/nexus/blob/master/LICENSE
[Maven Central]: http://repo2.maven.org/maven2/com/threerings/nexus/
