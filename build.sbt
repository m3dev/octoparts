scoverage.ScoverageSbtPlugin.instrumentSettings

// Exclude views and auto-generated code from coverage
ScoverageKeys.excludedPackages in ScoverageCompile := """com\.kenshoo.*;.*controllers\.javascript\..*;.*controllers\.ref\..*;.*controllers\.Reverse.*;.*BuildInfo.*;.*views\.html\..*;Routes"""

// ------------------------------
initialCommands := """
import com.m3.octoparts._
"""

