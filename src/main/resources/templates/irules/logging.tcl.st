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

when CLIENT_ACCEPTED {
  set cf_logging_handle [HSL::open -proto TCP -pool `logging_pool`]
}

when HTTP_REQUEST {
  set cf_logging_client_start [clock clicks -milliseconds]
  # LOG Client HTTP START
}

when HTTP_REQUEST_SEND {
  set cf_logging_server_start [clock clicks -milliseconds]
  # LOG Server HTTP START
}

when HTTP_RESPONSE {
  # Log Server HTTP STOP
}

when HTTP_RESPONSE_RELEASE {
  # Log Client HTTP STOP
}

proc send_http_start_event {client_server start_stop start_time} {

}
