/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.wso2.carbon.webapp.authenticator.framework;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.tomcat.ext.valves.CarbonTomcatValve;
import org.wso2.carbon.tomcat.ext.valves.CompositeValve;

import javax.servlet.http.HttpServletResponse;

public class WebappAuthenticatorFrameworkValve extends CarbonTomcatValve {

    private static final String AUTHENTICATION_SCHEME = "authentication-scheme";

    @Override
    public void invoke(Request request, Response response, CompositeValve compositeValve) {
        String authScheme =
                request.getContext().findParameter(WebappAuthenticatorFrameworkValve.AUTHENTICATION_SCHEME);
        if (authScheme == null || "".equals(authScheme)) {
            this.getNext().invoke(request, response, compositeValve);
            return;
        }
        WebappAuthenticator authenticator = WebappAuthenticatorFactory.getAuthenticator(authScheme);

        WebappAuthenticator.Status status = authenticator.authenticate(request, response);
        this.processResponse(request, response, compositeValve, status);
    }

    private void processResponse(Request request, Response response, CompositeValve compositeValve,
                                 WebappAuthenticator.Status status) {
        switch (status) {
            case SUCCESS:
            case CONTINUE:
                this.getNext().invoke(request, response, compositeValve);
                break;
            case FAILURE:
                AuthenticationFrameworkUtil.handleResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Failed to authorize the incoming request");
                break;
        }
    }

}
