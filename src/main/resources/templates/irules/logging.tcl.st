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

proc randInteger {max} {
    return [expr {int(rand()*$max)}]
}

proc uuid {} {
  set part1 [format %08x [call randInteger 0xffffffff]]
  set part2 [format %04x [call randInteger 0xffff]]
  set part3 "4[string range [format %04x [call randInteger 0xffff]] 0 2]"
  set part4 "8[string range [format %04x [call randInteger 0xffff]] 0 2]"
  set part5 [format %012x [call randInteger 0xffffffffffff]]
  return "$part1-$part2-$part3-$part4-$part5"
}

when CLIENT_ACCEPTED {
  set cf_logging_handle [HSL::open -proto TCP -pool `logging_pool`]
  set cf_logging_remote_address "[IP::remote_addr]:[TCP::remote_port]"
}

when LB_SELECTED {
  set cf_logging_server_address "[LB::server addr]:[LB::server port]"
}

when HTTP_REQUEST {
  set cf_logging_client_start [clock clicks -milliseconds]
  set cf_logging_host [HTTP::host]
  set cf_logging_method [HTTP::method]
  set cf_logging_uri [HTTP::uri]
  set cf_logging_http_version [HTTP::version]
  set cf_logging_referer [HTTP::header referer]
  if {[llength $cf_logging_referer] == 0} then {
    set cf_logging_referer "-"
  }
  set cf_logging_user_agent [HTTP::header user-agent]
  if {[llength $cf_logging_user_agent] == 0} then {
    set cf_logging_user_agent "-"
  }

  HTTP::header insert X-Request-Start $cf_logging_client_start
  set cf_requestid [call uuid]
  HTTP::header insert X-CF-RequestID $cf_requestid
  HTTP::header insert X-Vcap-Request-Id $cf_requestid
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
  set timestamp [clock format [expr $cf_logging_client_start / 1000] -format "%d/%b/%Y:%H:%M:%S %z"]
  set contentlength [HTTP::header "Content-Length"]
  if {[llength $contentlength] == 0} then {
    set contentlength "-"
  }
  set response_time [format %.03f [expr ([clock clicks -milliseconds] - $cf_logging_client_start) / 1000.0]]
  set log "$cf_logging_host - \[$timestamp\] \"$cf_logging_method $cf_logging_uri HTTP/$cf_logging_http_version\" [HTTP::status] $contentlength \"$cf_logging_referer\" \"$cf_logging_user_agent\" $cf_logging_remote_address vcap_request_id:$cf_requestid response_time:$response_time"
  HSL::send $cf_logging_handle "LOG dev-ltm $cf_logging_server_address $cf_logging_client_start $cf_requestid $log\n"
}

proc send_http_start_event {client_server start_stop start_time} {

}
