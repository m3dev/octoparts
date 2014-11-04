import sbt.Keys._
import sbt.Tests
import scoverage.ScoverageSbtPlugin._

object Scoverage {

  val settings =
    instrumentSettings ++
      Seq(
        ScoverageKeys.highlighting := true,
        ScoverageKeys.scoverageExcludedFiles := ".*(classes|src)_managed.*",
        testOptions in ScoverageTest += Tests.Argument("-u", "target/test-reports")
      )
}