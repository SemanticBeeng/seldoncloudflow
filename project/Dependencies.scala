import Versions._
import sbt._

object Dependencies {

  val grpcNetty         = "io.grpc"             % "grpc-netty-shaded"   % grpcVersion
  val grpcProtobuf      = "io.grpc"             % "grpc-protobuf"       % grpcVersion
  val grpcStub          = "io.grpc"             % "grpc-stub"           % grpcVersion
  val grpcCore          = "io.grpc"             % "grpc-core"             % grpcVersion
  val protobuf          = "com.google.protobuf" % "protobuf-java"       % protoVersion % "protobuf"
  val protobufutil      = "com.google.protobuf" % "protobuf-java-util"  % protoVersion

  val fabric8Client     = "io.fabric8"          % "kubernetes-client"     % fabric8Version

  val gson              = "com.google.code.gson"% "gson"                % gsonVersion
  val scalajHTTP        = "org.scalaj"          %% "scalaj-http"        % scalajHTTPVersion
  val logback           = "ch.qos.logback"      % "logback-classic"     % logbackVersion
  val akkaHttpJsonJackson= "de.heikoseeberger"  %% "akka-http-jackson"  % akkaHttpJsonVersion

  val tensorFlow        = "org.tensorflow"      % "tensorflow"          % tensorflowVersion
  val tensorFlowProto   = "org.tensorflow"      % "proto"               % tensorflowVersion

  val minio             = "io.minio"            % "minio"               % minioVersion

  val typesafeConfig    = "com.typesafe"        %  "config"             % TypesafeConfigVersion
  val ficus             = "com.iheart"          %% "ficus"              % FicusVersion

  val scalaTest         = "org.scalatest"       %% "scalatest"          % scaltestVersion    % "test"

  val grpcDependencies = Seq(grpcNetty, grpcProtobuf, grpcStub, grpcCore, protobuf, protobufutil)
}
