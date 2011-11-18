import sbt._
import Keys._

object NexusBuild extends Build {
  // common build configuration
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization     := "com.threerings.nexus",
    version          := "1.0-SNAPSHOT",
    crossPaths       := false,
    javacOptions     ++= Seq("-Xlint", "-Xlint:-serial", "-source", "1.6", "-target", "1.6"),
    fork in Compile  := true,
    resolvers        += "Local Maven Repository" at Path.userHome.asURL + "/.m2/repository",
    scalaVersion     := "2.9.0-1",
    autoScalaLibrary := false, // no scala-library dependency
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.+" % "test",
 	    "com.novocode" % "junit-interface" % "0.7" % "test->default"
    )
  )

  // GWT bits
  val gwtVers = "2.3.0"
  val gwtSettings = Seq[Setting[_]](
    // include the sources in classes/ and the jar file
    unmanagedResourceDirectories in Compile <+= baseDirectory / "src/main/java",
    // TODO: how to do? unmanagedSourceDirectories in Compile <+= javaSource in Compile,
    unmanagedResources in Compile ~= (_.filterNot(_.isDirectory)) // work around SBT bug
  )
  System.setProperty("gwt.args", "-war target/test-war")

  //
  // sub-project definitions

  def subProject (id :String, extraSettings :Seq[Setting[_]] = Seq()) = Project(
    id, file(id), settings = buildSettings ++ extraSettings ++ Seq(
      name := "nexus-" + id
    )
  )

  // core projects
  val coreLocals = new com.samskivert.condep.Depends(
    ("react", null,  "com.threerings" % "react" % "1.1")
  )
  lazy val core = coreLocals.addDeps(subProject("core", gwtSettings ++ Seq(
    libraryDependencies ++= coreLocals.libDeps
  )))
  lazy val testSupport = subProject("test-support") dependsOn(core)
  lazy val server = subProject("server", Seq(
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "10.0.1"
    )
  )) dependsOn(testSupport)

  // gwt-backend projects
  lazy val gwtIO = subProject("gwt-io", gwtSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.google.gwt" % "gwt-user" % gwtVers % "provided",
      "com.google.gwt" % "gwt-dev" % gwtVers % "provided",
      "javax.validation" % "validation-api" % "1.0.0.GA",
      "javax.validation" % "validation-api" % "1.0.0.GA" classifier "sources"
    )
  )) dependsOn(testSupport)
  lazy val gwtServer = subProject("gwt-server", Seq(
    libraryDependencies ++= Seq(
      "org.eclipse.jetty" % "jetty-servlet" % "7.4.3.v20110701",
      "org.eclipse.jetty" % "jetty-websocket" % "7.4.3.v20110701",
      "com.google.gwt" % "gwt-user" % gwtVers % "provided",
      "com.google.gwt" % "gwt-dev" % gwtVers % "provided"
    )
  )) dependsOn(server, gwtIO)

  // jvm-backend projects
  lazy val jvmIO = subProject("jvm-io", Seq(
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "10.0.1"
    )
  )) dependsOn(testSupport)
  lazy val jvmServer = subProject("jvm-server") dependsOn(server, jvmIO)

  // tools project
  lazy val tools = subProject("tools", Seq(
    scalacOptions    ++= Seq("-unchecked", "-deprecation"),
    autoScalaLibrary := true, // we want scala-library back
    libraryDependencies ++= Seq(
      "com.samskivert" % "jmustache" % "1.4"
    )
  )) dependsOn(core)

  //
  // demo projects

  def demoProject (id :String, extraSettings :Seq[Setting[_]] = Seq()) = Project(
    id, file("demos/" + id), settings = buildSettings ++ extraSettings ++ Seq(
      name := "nexus-demo-" + id
    )
  )

  lazy val chat = demoProject("chat", Seq(
    libraryDependencies ++= Seq(
      "com.samskivert" % "samskivert" % "1.2",
      "com.threerings" % "gwt-utils" % "1.2"
    )
  )) dependsOn(jvmServer, gwtServer)

  //
  // one giant fruit roll-up to bring them all together

  lazy val nexus = Project("nexus", file(".")) aggregate(
    core, testSupport, server, gwtIO, gwtServer, jvmIO, jvmServer, tools, chat)
}
