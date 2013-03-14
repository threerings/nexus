import sbt._
import Keys._
import samskivert.ProjectBuilder

object NexusBuild extends Build {
  val metaPOM = pomutil.POM.fromFile(file("pom.xml")).get
  val builder = new ProjectBuilder("pom.xml") {
    override val globalSettings = Seq(
      crossPaths    := false,
      scalaVersion  := metaPOM.properties("scala.version"),
      scalacOptions ++= Seq("-unchecked", "-deprecation"),
      autoScalaLibrary in Compile := false, // no scala-library dependency (except for tests)
      javacOptions  ++= Seq("-Xlint", "-Xlint:-serial", "-source", "1.6", "-target", "1.6"),
      javaOptions   ++= Seq("-ea"),
      fork in Compile := true,
      publishArtifact in (Compile, packageDoc) := false, // no scaladocs; it fails
      libraryDependencies ++= Seq(
        "com.novocode" % "junit-interface" % "0.8" % "test->default" // make junit work
      )
    )
    override def projectSettings (name :String) = name match {
      case "core" => Seq(
        // adds source files to our jar file (needed by GWT)
        unmanagedResourceDirectories in Compile <+= baseDirectory / "src/main/java"
        // unmanagedBase <<= baseDirectory { base => base / "disabled" }
      )
      case "gwt-io" => Seq(
        // adds source files to our jar file (needed by GWT)
        unmanagedResourceDirectories in Compile <+= baseDirectory / "src/main/java"
        // unmanagedBase <<= baseDirectory { base => base / "disabled" }
      )
      case "jvm-server" => Seq(
        // server tests listen on sockets, so we need to run them serially
        parallelExecution in Test := false
      )
      case "tools" => Seq(
        autoScalaLibrary := true // we want scala-library back
      )
      case _ => Nil
    }
  }

  // make GWT unit tests not take craps in top-level war/ directory
  System.setProperty("gwt.args", "-war target/test-war")

  lazy val core = builder("core")
  lazy val testSupport = builder("test-support")
  lazy val server = builder("server")
  lazy val gwtIO = builder("gwt-io")
  lazy val gwtServer = builder("gwt-server")
  lazy val jvmIO = builder("jvm-io")
  lazy val jvmServer = builder("jvm-server")
  lazy val tools = builder("tools")

  // def demoProject (id :String, extraSettings :Seq[Setting[_]] = Seq()) = Project(
  //   id, file("demos/" + id), settings = buildSettings ++ extraSettings ++ Seq(
  //     name := "nexus-demo-" + id
  //   )
  // )

  // lazy val chat = demoProject("chat", Seq(
  //   libraryDependencies ++= Seq(
  //     "com.samskivert" % "samskivert" % "1.2",
  //     "com.threerings" % "gwt-utils" % "1.2"
  //   )
  // )) dependsOn(jvmServer, gwtServer)

  // one giant fruit roll-up to bring them all together
  lazy val nexus = Project("nexus", file(".")) aggregate(
    core, testSupport, server, gwtIO, gwtServer, jvmIO, jvmServer, tools/*, chat*/)
}
