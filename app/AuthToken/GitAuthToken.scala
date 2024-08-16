package AuthToken


object AuthToken extends App {
  val authToken: String = sys.env.getOrElse("AuthPassword", "")

  if (authToken.isEmpty) {
    println("AuthPassword environment variable is not set.")
  } else {
    println(s"AuthPassword is set. Token: $authToken")
  }

}



