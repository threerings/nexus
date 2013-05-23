import sbt._
import Keys._

object NexusBuild extends samskivert.MavenBuild {

  override val globalSettings = Seq(
    crossPaths    := false,
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

  override def moduleSettings (name :String, pom :pomutil.POM) = name match {
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

  // make GWT unit tests not take craps in top-level war/ directory
  System.setProperty("gwt.args", "-war target/test-war")
}
