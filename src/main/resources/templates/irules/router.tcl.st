#
# This iRule is updated programmatically; any changes will be overwritten.
#
when HTTP_REQUEST {
  set host [getfield [HTTP::host] ":" 1]
  set cfPool [string tolower "`poolNamePrefix`$host"]
  set body404Content "<html><head><title>404 Not Found</title></head><body>
    <div style=\"text-align: center;\">
      <h1>404 Not Found</h1>
      <p>Could not find route for <b>$host</b> in Cloud Foundry.</p>
    </div>
    </body>
    </html>"
  if {([ catch { pool $cfPool } ]) || ([active_members $cfPool] == 0)} {
    HTTP::respond 404 content $body404Content
  } else {
    pool $cfPool
  }
}
