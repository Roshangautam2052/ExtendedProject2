package AuthToken


object AuthToken  {
  val authToken: String = sys.env.getOrElse("AuthPassword", "")
}



