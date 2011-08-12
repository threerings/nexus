import sbt._
import Keys._

// allows projects to be symlinked into the current directory for a direct dependency,
// or fall back to obtaining the project from Maven otherwise
class Locals (locals :(String, String, ModuleID)*) {
  def addDeps (p :Project) = (locals collect {
    case (id, subp, dep) if (file(id).exists) => symproj(file(id), subp)
  }).foldLeft(p) { _ dependsOn _ }
  def libDeps = locals collect {
    case (id, subp, dep) if (!file(id).exists) => dep
  }
  private def symproj (dir :File, subproj :String = null) =
    if (subproj == null) RootProject(dir) else ProjectRef(dir, subproj)
}

object ForPlayBuild extends Build {
  val locals = new Locals(
    ("react",      null,  "com.threerings" % "react" % "1.0-SNAPSHOT")
  )

  // common build configuration
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization     := "com.threerings",
    version          := "1.0-SNAPSHOT",
    crossPaths       := false,
    javacOptions     ++= Seq("-Xlint", "-Xlint:-serial"),
    fork in Compile  := true,
    resolvers        += "Local Maven Repository" at Path.userHome.asURL + "/.m2/repository",
    autoScalaLibrary := false, // no scala-library dependency
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.+" % "test",
 	    "com.novocode" % "junit-interface" % "0.7" % "test->default"
    )
  )

  // GWT bits
  val gwtVers = "2.3.0"
  System.setProperty("gwt.args", "-war target/test-war")

  // sub-project definitions
  def subProject (id :String, extraSettings :Seq[Setting[_]] = Seq()) = Project(
    id, file(id), settings = buildSettings ++ extraSettings ++ Seq(
      name := "nexus-" + id,
      // include the sources in classes/ and the jar file
      unmanagedResourceDirectories in Compile <+= baseDirectory / "src/main/java",
      // TODO: how to do? unmanagedSourceDirectories in Compile <+= javaSource in Compile,
      unmanagedResources in Compile ~= (_.filterNot(_.isDirectory)) // work around SBT bug
    )
  )

  // core projects
  lazy val core = locals.addDeps(subProject("core", Seq(
    libraryDependencies ++= locals.libDeps
  )))
  lazy val testSupport = subProject("test-support") dependsOn(core)
  lazy val server = subProject("server", Seq(
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "r09"
    )
  )) dependsOn(testSupport)

  // gwt-backend projects
  lazy val gwtIO = subProject("gwt-io", Seq(
    libraryDependencies ++= Seq(
      "com.google.gwt" % "gwt-user" % gwtVers,
      "com.google.gwt" % "gwt-dev" % gwtVers,
      "javax.validation" % "validation-api" % "1.0.0.GA",
      "javax.validation" % "validation-api" % "1.0.0.GA" classifier "sources"
    )
  )) dependsOn(testSupport)
  lazy val gwtServer = subProject("gwt-server", Seq(
    libraryDependencies ++= Seq(
      "org.eclipse.jetty" % "jetty-servlet" % "8.0.0.M2",
      "org.eclipse.jetty" % "jetty-websocket" % "8.0.0.M2",
      "com.google.gwt" % "gwt-dev" % gwtVers // TODO: provided
    )
  )) dependsOn(server, gwtIO)

  // jvm-backend projects
  lazy val jvmIO = subProject("jvm-io", Seq(
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "r09"
    )
  )) dependsOn(testSupport)
  lazy val jvmServer = subProject("jvm-server") dependsOn(server, jvmIO)

  // one giant fruit roll-up to bring them all together
  lazy val nexus = Project("nexus", file(".")) aggregate(
    core, testSupport, server, gwtIO, gwtServer, jvmIO, jvmServer)

  // demo projects

  // lazy val text = Project(
  //   "text-sample", file("sample/text"), settings = buildSettings ++ Seq(
  //     name := "text-sample",
  //     unmanagedSourceDirectories in Compile <+= baseDirectory / "core/src"
  //   )
  // ) dependsOn(core)
}
