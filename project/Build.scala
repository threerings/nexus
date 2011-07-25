import sbt._
import Keys._
import java.io.File

object ForPlayBuild extends Build {
  // common build configuration
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization    := "com.threerings",
    version         := "1.0-SNAPSHOT",
    crossPaths      := false,
    javacOptions    ++= Seq("-Xlint", "-Xlint:-serial"),
    fork in Compile := true,
    resolvers       += "Local Maven Repository" at Path.userHome.asURL + "/.m2/repository",
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.+" % "test",
 	    "com.novocode" % "junit-interface" % "0.7" % "test->default"
    )
  )

  // GWT bits
  val gwtVers = "2.3.0"
  System.setProperty("gwt.args", "-war target/test-war")

  // define a task for copying sources into classes directory + jar file
  val copySources = TaskKey[Seq[(File,File)]](
    "copy-sources", "Copies sources to the output directory.")
  def copySourcesTask = (classDirectory, cacheDirectory, javaSource,
                         defaultExcludes in unmanagedSources, streams) map {
    (target, cache, srcdir, excls, s) =>
    val dirs = Seq(srcdir)
    val srcs = dirs.descendentsExcept("*.java", excls)
    val mappings = srcs x (rebase(dirs, target) | flat(target))
    s.log.debug("Copy source mappings: " + mappings.mkString("\n\t","\n\t",""))
    Sync(cache / "copy-sources")( mappings )
    mappings
  }
  val csBareSettings = Seq(
    copySources <<= copySourcesTask,
    copyResources <<= copyResources.dependsOn(copySources)
  )
  val csSettings = inConfig(Compile)(csBareSettings) ++ inConfig(Test)(csBareSettings)

  // sub-project definitions
  def subProject (id :String, extraSettings :Seq[Setting[_]] = Seq()) = Project(
    id, file(id), settings = buildSettings ++ csSettings ++ extraSettings ++ Seq(
      name := "nexus-" + id
    )
  )

  // core projects
  lazy val core = subProject("core")
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
