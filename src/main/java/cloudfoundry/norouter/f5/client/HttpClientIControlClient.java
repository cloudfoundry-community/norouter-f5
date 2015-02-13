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
package cloudfoundry.norouter.f5.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class HttpClientIControlClient extends AbstractIControlClient {

	private static Logger LOGGER = LoggerFactory.getLogger(HttpClientIControlClient.class);

	private final CloseableHttpClient httpClient;

	public static Builder create() {
		return new Builder();
	}

	public static class Builder {

		private boolean verifyTls = true;
		private URI address;
		private String user;
		private String password;

		public Builder address(String address) {
			return address(URI.create(address));
		}

		public Builder address(URI address) {
			Objects.requireNonNull(address);
			final String userInfo = address.getUserInfo();
			if (userInfo != null) {
				final int seperatorIndex = userInfo.indexOf(':');
				this.user = userInfo.substring(0, seperatorIndex);
				this.password = userInfo.substring(seperatorIndex + 1);
			}
			this.address = address;
			return this;
		}

		public Builder verifyTls(boolean verifyTls) {
			this.verifyTls = verifyTls;
			return this;
		}

		public Builder user(String user) {
			this.user = user;
			return this;
		}

		public Builder password(String password) {
			this.password = password;
			return this;
		}

		public IControlClient build() {
			Objects.requireNonNull(address, "address is a required argument");
			final HttpHost host = new HttpHost(address.getHost(), address.getPort(), address.getScheme());
			return new HttpClientIControlClient(host, this);
		}

	}

	private HttpClientIControlClient(HttpHost host, Builder builder) {
		super(URI.create(host.toURI()));

		final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
				Objects.requireNonNull(builder.user, "user is a required argument"),
				Objects.requireNonNull(builder.password, "password is a required argument"));
		credentialsProvider.setCredentials(new AuthScope(host), credentials);

		final HttpClientBuilder httpClientBuilder = HttpClients.custom()
				.setDefaultCredentialsProvider(credentialsProvider)
				.disableCookieManagement();

		if (!builder.verifyTls) {
			httpClientBuilder.setSSLHostnameVerifier((s, sslSession) -> true);
			httpClientBuilder.setSSLSocketFactory(NaiveTrustManager.getSocketFactory());
		}
		httpClient = httpClientBuilder.build();
	}

	@Override
	public void close() {
		HttpClientUtils.closeQuietly(httpClient);
	}

	@Override
	protected JsonNode getResource(String uri) {
		LOGGER.debug("GETting {}", uri);
		final HttpGet request = new HttpGet(address.resolve(uri));
		try (final CloseableHttpResponse response = httpClient.execute(request)) {
			final StatusLine statusLine = response.getStatusLine();
			validateResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase(), 200);
			return mapper.readTree(response.getEntity().getContent());
		} catch (IOException e) {
			throw new IControlException(e);
		}
	}

	@Override
	protected JsonNode postResource(String uri, Object resource) {
		try {
			final String body = mapper.writeValueAsString(resource);
			LOGGER.debug("POSTting {} {}", uri, body);
			final HttpPost request = new HttpPost(address.resolve(uri));
			request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
			try (final CloseableHttpResponse response = httpClient.execute(request)) {
				final StatusLine statusLine = response.getStatusLine();
				validateResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase(), 200);
				return mapper.readTree(response.getEntity().getContent());
			}
		} catch (IOException e) {
			throw new IControlException(e);
		}
	}

	@Override
	protected void deleteResource(String uri) {
		try {
			LOGGER.debug("DELETEing {}", uri);
			final HttpDelete request = new HttpDelete(address.resolve(uri));
			try (final CloseableHttpResponse response = httpClient.execute(request)) {
				final StatusLine statusLine = response.getStatusLine();
				validateResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase(), 200);
			}
		} catch (IOException e) {
			throw new IControlException(e);
		}
	}

}
