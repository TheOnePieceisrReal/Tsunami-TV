package app.cinestream.data.parser

/** Builds standard Xtream-style endpoints from user-supplied server/user/pass. No provider is hardcoded. */
object XtreamHelper {
    private fun base(server: String) = server.trim().trimEnd('/')
    fun m3uUrl(server: String, user: String, pass: String) =
        "${base(server)}/get.php?username=$user&password=$pass&type=m3u_plus&output=ts"
    fun epgUrl(server: String, user: String, pass: String) =
        "${base(server)}/xmltv.php?username=$user&password=$pass"
}
