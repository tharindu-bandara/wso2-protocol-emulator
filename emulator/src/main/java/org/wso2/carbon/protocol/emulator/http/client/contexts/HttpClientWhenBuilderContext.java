/*
 * *
 *  * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.protocol.emulator.http.client.contexts;

import org.wso2.carbon.protocol.emulator.dsl.contexts.AbstractWhenBuilderContext;

import java.util.List;

/**
 * Http client when builder context.
 */
public class HttpClientWhenBuilderContext extends AbstractWhenBuilderContext<HttpClientRequestBuilderContext> {
    private List<HttpClientWhenBuilderContext> whenBuilderContextList;
    private HttpClientInformationContext httpClientInformationContext;
    private HttpClientThenBuilderContext thenBuilderContext;

    public HttpClientWhenBuilderContext(List<HttpClientWhenBuilderContext> whenBuilderContextList,
            HttpClientInformationContext httpClientInformationContext) {
        this.httpClientInformationContext = httpClientInformationContext;
        this.whenBuilderContextList = whenBuilderContextList;
        this.whenBuilderContextList.add(this);
    }

    @Override
    public HttpClientThenBuilderContext when(HttpClientRequestBuilderContext requestContext) {
        httpClientInformationContext.setRequestContext(requestContext);
        thenBuilderContext = new HttpClientThenBuilderContext(whenBuilderContextList, requestContext,
                httpClientInformationContext);
        return thenBuilderContext;
    }

    @Override
    public HttpClientOperationBuilderContext operation() {
        return new HttpClientOperationBuilderContext(httpClientInformationContext);
    }
}
