package presentation

/**
 * A collection of links to show in the navbar of the admin UI
 */
case class NavbarLinks(
  kibana: Option[String],
  hystrixDashboard: Option[String],
  swaggerUI: Option[String],
  wiki: Option[String]
)
