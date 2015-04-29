#
# This iRule is updated programmatically; any changes will be overwritten.
#

#
# Copyright (c) 2015 Intellectual Reserve, Inc.  All rights reserved.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

# TODO Add support for wildcard routes
when HTTP_REQUEST {
  set host [getfield [HTTP::host] ":" 1]
  set cfPool [string tolower "`poolNamePrefix`$host"]
  if {([ catch { pool $cfPool } ]) || ([active_members $cfPool] == 0)} {
    set body404Content "<html><head><title>404 Not Found</title></head><body>
      <div style=\"text-align: center;\">
        <h1>404 Not Found</h1>
        <p>Could not find route for <b>$host</b> in Cloud Foundry.</p>
      </div>
      </body>
      </html>"
    HTTP::respond 404 content $body404Content
    event disable all
  }
}

when LB_FAILED {
    set body502Content "<html><head><title>502 Error unable to connect to server</title></head><body>
      <div style=\"text-align: center;\">
        <h1>502 Unable to connect to app</h1>
      </div>
      </body>
      </html>"
    HTTP::respond 502 content $body502Content
    event disable all
}
