/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.protocol.emulator.http.client.processors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.apache.log4j.Logger;
import org.wso2.carbon.protocol.emulator.http.client.contexts.HttpClientConfigBuilderContext;
import org.wso2.carbon.protocol.emulator.http.client.contexts.HttpClientRequestBuilderContext;
import org.wso2.carbon.protocol.emulator.http.client.contexts.HttpClientRequestProcessorContext;
import org.wso2.carbon.protocol.emulator.http.params.Cookie;
import org.wso2.carbon.protocol.emulator.http.params.Header;
import org.wso2.carbon.protocol.emulator.http.params.QueryParameter;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Class to process the Request information of client.
 */
public class HttpRequestInformationProcessor {

    private static final Logger log = Logger.getLogger(HttpRequestInformationProcessor.class);

    public static void process(HttpClientRequestProcessorContext processorContext) {
        HttpClientConfigBuilderContext clientConfigBuilderContext = processorContext.getClientInformationContext()
                .getClientConfigBuilderContext();
        String uri = getURI(clientConfigBuilderContext.getHost(), clientConfigBuilderContext.getPort(),
                processorContext.getRequestBuilderContext());

        URI requestUri = null;

        try {
            requestUri = new URI(uri);
        } catch (URISyntaxException e) {
            log.error(e);
        }
        processorContext.getClientInformationContext().getClientConfigBuilderContext().host(requestUri.getHost());
        String scheme = requestUri.getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            log.error("Only HTTP(S) is supported.");
        }

        HttpVersion httpVersion = processorContext.getRequestBuilderContext().getHttpVersion();

        ByteBuf content;
        HttpRequest request;
        if (processorContext.getRequestBuilderContext().getBody() != null) {

            String rawData = processorContext.getRequestBuilderContext().getBody();
            byte[] bytes = rawData.getBytes(Charset.defaultCharset());
            content = Unpooled.wrappedBuffer(bytes);
            request = new DefaultFullHttpRequest(httpVersion,
                                                 processorContext.getRequestBuilderContext().getMethod(),
                                                 requestUri.getRawPath(),
                                                 content);
        } else {
            request = new DefaultFullHttpRequest(httpVersion,
                    processorContext.getRequestBuilderContext().getMethod(), requestUri.getRawPath());
        }

        processorContext.setRequest(request);
        populateHeader(processorContext);
        populateCookies(processorContext);
        populateQueryParameters(processorContext);
    }

    private static void populateHeader(HttpClientRequestProcessorContext processorContext) {

        HttpRequest request = processorContext.getRequest();
        HttpClientRequestBuilderContext requestContext = processorContext.getRequestBuilderContext();
        HttpClientConfigBuilderContext configBuilderContext = processorContext.getClientInformationContext()
                                                                                    .getClientConfigBuilderContext();
        request.headers().set(HttpHeaders.Names.HOST,
                              configBuilderContext.getHost());

        if (configBuilderContext.isKeepAlive()) {
            request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        } else {
            request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        }

        if (requestContext.getBody() != null) {
            if (requestContext.isChunkingEnabled()) {
                HttpHeaders.setTransferEncodingChunked(request);
            } else {
                request.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
                                      requestContext.getBody().getBytes(Charset.defaultCharset()).length);
            }
        }
        if (requestContext.getHeaders() != null) {
            for (Header header : requestContext.getHeaders()) {
                request.headers().set(header.getName(), header.getValue());
            }
        }
    }

    private static void populateCookies(HttpClientRequestProcessorContext processorContext) {

        HttpClientRequestBuilderContext requestContext = processorContext.getRequestBuilderContext();
        if (requestContext.getCookies() != null) {
            DefaultCookie[] cookies = new DefaultCookie[requestContext.getCookies().size()];
            int i = 0;
            for (Cookie cookie : requestContext.getCookies()) {
                cookies[i++] = new DefaultCookie(cookie.getName(), cookie.getValue());
            }
            processorContext.getRequest().headers()
                    .set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.STRICT.encode(cookies));
        }
    }

    private static void populateQueryParameters(HttpClientRequestProcessorContext processorContext) {

        HttpRequest request = processorContext.getRequest();
        List<QueryParameter> queryParameters = processorContext.getClientInformationContext().getRequestContext()
                .getQueryParameters();
        String uri = request.getUri();

        if (queryParameters != null) {
            String query = "?";

            for (QueryParameter q : queryParameters) {
                query = query.concat(q.getName());
                query = query.concat("=");
                query = query.concat(q.getValue());
                query = query.concat("&");
            }
            query = query.substring(0, query.length() - 1);
            uri = uri.concat(query);
            request.setUri(uri);
        }

    }

    private static String getURI(String host, int port, HttpClientRequestBuilderContext requestBuilderContext) {
        String httpSchema = "http://";
        String path = requestBuilderContext.getPath();
        String uri = host + ":" + port;

        if (path == null) {
            uri = httpSchema + uri + "/";
            return uri;
        }

        if (path.startsWith("/")) {
            uri = uri + path;
        } else {
            uri = uri + "/" + path;
        }

        if (!uri.startsWith(httpSchema)) {
            uri = httpSchema + uri;
        }
        return uri;
    }
}
