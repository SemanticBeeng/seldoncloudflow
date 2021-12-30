addSbtPlugin("org.scalariform"  %  "sbt-scalariform" % "1.8.2")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.7"

evictionErrorLevel := Level.Info
