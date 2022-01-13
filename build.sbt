import sbt._
import sbt.Keys._
import Dependencies._
import scalariform.formatter.preferences._

name := "TFGRPC"

lazy val thisVersion = "0.1"

scalaVersion in ThisBuild := "2.12.10"
version in ThisBuild := thisVersion
test in ThisBuild := {}

lazy val protocols =  (project in file("./protocol"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ) ++ grpcDependencies
  )

lazy val support = (project in file("./support"))
  .enablePlugins(CloudflowAkkaPlugin)
  .settings(
    name := "support",
    version := thisVersion,
    libraryDependencies ++= Seq(gson, scalajHTTP, akkaHttpJsonJackson, fabric8Client, tensorFlow, tensorFlowProto, minio, typesafeConfig, ficus, logback, scalaTest),
  )
  .dependsOn(protocols)
  .settings(commonSettings)

lazy val grpcClient =  (project in file("./grpcclient"))
  .dependsOn(support)
  .settings(commonSettings)


lazy val SeldonGRPCModelServing = (project in file("./seldon-grpc"))
  .enablePlugins(CloudflowApplicationPlugin, CloudflowAkkaPlugin)
  .settings(
    name := "seldon-grpc",
    version := thisVersion
  )
  .settings(commonSettings)
  .dependsOn(support)

lazy val SeldonRESTModelServing = (project in file("./seldon-rest"))
  .enablePlugins(CloudflowApplicationPlugin, CloudflowAkkaPlugin)
  .settings(
    name := "seldon-rest",
    version := thisVersion
  )
  .settings(commonSettings)
  .dependsOn(support)

lazy val FraudGRPCModelServing = (project in file("./fraud-grpc"))
  .enablePlugins(CloudflowApplicationPlugin,CloudflowAkkaPlugin)
  .settings(
    name := "fraud-grpc",
    version := thisVersion
  )
  .settings(commonSettings)
  .dependsOn(support)

lazy val FraudRESTModelServing = (project in file("./fraud-rest"))
  .enablePlugins(CloudflowApplicationPlugin,CloudflowAkkaPlugin)
  .settings(
    name := "fraud-rest",
    version := thisVersion
  )
  .settings(commonSettings)
  .dependsOn(support)

lazy val commonScalacOptions = Seq(
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Xlog-reflective-calls",
  "-Xlint:_",
  "-deprecation",
  "-feature",
  "-language:_",
  "-unchecked"
)

lazy val scalacTestCompileOptions = commonScalacOptions ++ Seq(
//  "-Xfatal-warnings",                  // Avro generates unused imports, so this is commented out not to break build
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
  "-Ywarn-unused:params",              // Warn if a value parameter is unused. (But there's no way to suppress warning when legitimate!!)
  "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",            // Warn if a private member is unused.
)

lazy val scalacSrcCompileOptions = scalacTestCompileOptions ++ Seq(
  "-Ywarn-value-discard")

lazy val commonSettings = Seq(
  scalacOptions in Compile := scalacSrcCompileOptions,
  scalacOptions in Test := scalacTestCompileOptions,
  scalacOptions in (Compile, console) := commonScalacOptions,
  scalacOptions in (Test, console) := commonScalacOptions,

  evictionErrorLevel := Level.Info,

  scalariformPreferences := scalariformPreferences.value
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 90)
    .setPreference(DoubleIndentConstructorArguments, true)
    .setPreference(DoubleIndentMethodDeclaration, true)
    .setPreference(IndentLocalDefs, true)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(DanglingCloseParenthesis, Preserve)
    .setPreference(NewlineAtEndOfFile, true)
    .setPreference(AllowParamGroupsOnNewlines, true)
    .setPreference(SpacesWithinPatternBinders, false) // otherwise case head +: tail@_ fails to compile!
)
