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
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.IllegalFormatCodePointException;
import java.util.Objects;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class HttpClientIControlClient extends AbstractIControlClient {

	private static final String APPLICATION_JSON = "application/json";
	private static Logger LOGGER = LoggerFactory.getLogger(HttpClientIControlClient.class);

	private final HttpHost host;
	private final CloseableHttpClient httpClient;
	private final ThreadLocal<HttpClientContext> context = new ThreadLocal<HttpClientContext>() {
		@Override
		protected HttpClientContext initialValue() {
			final HttpClientContext context = HttpClientContext.create();

			final AuthCache authCache = new BasicAuthCache();
			final BasicScheme basicAuth = new BasicScheme();
			authCache.put(host, basicAuth);
			context.setAuthCache(authCache);

			return context;
		}
	};
	public static Builder create() {
		return new Builder();
	}

	public static class Builder {

		private boolean skipVerifyTls = false;
		private URI url;
		private String user;
		private String password;

		public Builder url(String address) {
			return url(URI.create(address));
		}

		public Builder url(URI address) {
			Objects.requireNonNull(address);
			final String userInfo = address.getUserInfo();
			if (userInfo != null) {
				final int separatorIndex = userInfo.indexOf(':');
				this.user = userInfo.substring(0, separatorIndex);
				this.password = userInfo.substring(separatorIndex + 1);
			}
			this.url = address;
			return this;
		}

		public Builder skipVerifyTls(boolean verifyTls) {
			this.skipVerifyTls = verifyTls;
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

		public HttpClientIControlClient build() {
			Objects.requireNonNull(url, "url is a required argument");
			final HttpHost host = new HttpHost(url.getHost(), url.getPort(), url.getScheme());
			return new HttpClientIControlClient(host, this);
		}

	}

	private HttpClientIControlClient(HttpHost host, Builder builder) {
		super(URI.create(host.toURI()));
		this.host = host;

		final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
				Objects.requireNonNull(builder.user, "user is a required argument"),
				Objects.requireNonNull(builder.password, "password is a required argument"));
		credentialsProvider.setCredentials(new AuthScope(host.getHostName(), host.getPort()), credentials);

		final HttpClientBuilder httpClientBuilder = HttpClients.custom()
				.setUserAgent("curl/7.37.1")
				.disableCookieManagement()
				.setDefaultCredentialsProvider(credentialsProvider);
		if (builder.skipVerifyTls) {
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
		try (final CloseableHttpResponse response = httpClient.execute(request, context.get())) {
			final StatusLine statusLine = response.getStatusLine();
			final String responseBody = StreamUtils.copyToString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			validateResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase(), responseBody, 200);
			return mapper.readTree(responseBody);
		} catch (IOException e) {
			throw new IControlException(e);
		}
	}

	@Override
	protected JsonNode postResource(String uri, Object resource) {
		return update(uri, resource, Method.POST);
	}

	@Override
	protected JsonNode putResource(String uri, Object resource) {
		return update(uri, resource, Method.PUT);
	}

	@Override
	protected JsonNode patchResource(String uri, Object resource) {
		return update(uri, resource, Method.PATCH);
	}

	private enum Method { PUT, POST, PATCH }

	private JsonNode update(String uri, Object resource, Method method) {
		try {
			final String body = mapper.writeValueAsString(resource);
			LOGGER.debug("{}ting {} {}", method, uri, body);
			final HttpEntityEnclosingRequestBase request;
			final URI resolvedUri = address.resolve(uri);
			switch (method) {
				case PATCH:
					request = new HttpPatch(resolvedUri);
					break;
				case POST:
					request = new HttpPost(resolvedUri);
					break;
				case PUT:
					request = new HttpPut(resolvedUri);
					break;
				default:
					throw new IllegalStateException("No handler for: " + method);
			}
			request.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
			request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
			try (final CloseableHttpResponse response = httpClient.execute(request, context.get())) {
				final StatusLine statusLine = response.getStatusLine();
				final String responseBody = StreamUtils.copyToString(response.getEntity().getContent(), StandardCharsets.UTF_8);
				validateResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase(), responseBody, 200);
				return mapper.readTree(responseBody);
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
				validateResponse(statusLine.getStatusCode(), statusLine.getReasonPhrase(), "", 200);
			}
		} catch (IOException e) {
			throw new IControlException(e);
		}
	}

}
