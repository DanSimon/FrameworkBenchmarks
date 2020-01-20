//resolvers += "scalene" at "https://maven.pkg.github.com/DanSimon/Scalene"

val ScaleneVersion = "0.1.0-SNAPSHOT"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "io.dsimon" %% "scalene" % ScaleneVersion,
  "io.dsimon" %% "scalene-routing" % ScaleneVersion,
  "io.dsimon" %% "scalene-sql" % ScaleneVersion,
  "org.postgresql" % "postgresql"        % "42.2.0",
  "org.json4s"                   %% "json4s-jackson"       % "3.5.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.2"

)

