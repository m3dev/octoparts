import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences.{DoubleIndentClassDeclaration, AlignParameters}

object Scalariform {

  val settings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)
  )

}