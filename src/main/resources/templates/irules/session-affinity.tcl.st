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

when HTTP_RESPONSE {
  if { [HTTP::cookie exists "JSESSIONID"] } {
    persist add uie [HTTP::cookie "JSESSIONID"]
  }
}
when HTTP_REQUEST {
  if { [HTTP::cookie exists "JSESSIONID"] } {
    persist uie [HTTP::cookie "JSESSIONID"]
  }
}
