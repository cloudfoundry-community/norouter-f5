/*
 * Copyright (c) 2015 Intellectual Reserve, Inc.  All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cloudfoundry.norouter.f5;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * @author Mike Heath
 */
class Json {

	private static final ObjectMapper mapper = new ObjectMapper()
			.findAndRegisterModules()
			.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

	public static <T> T fromJson(Class<T> type, String json) {
		try {
			return mapper.reader(type).readValue(json);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String toJson(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String escape(String json) {
		return json.replaceAll("\"", "`");
	}

	public static String unescape(String jsonish) {
		return jsonish.replaceAll("`", "\"");
	}
}
