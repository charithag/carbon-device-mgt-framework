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
package org.wso2.carbon.identity.oauth.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.oauth.OAuthAdminService;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import org.wso2.carbon.identity.oauth.extension.profile.RegistrationProfile;

import java.util.Arrays;

public class DynamicClientRegistrationUtil {

    private static final Log log = LogFactory.getLog(DynamicClientRegistrationUtil.class);

    public static OAuthApplicationInfo registerApplication(RegistrationProfile profile) throws APIManagementException {
        OAuthApplicationInfo oAuthApplicationInfo = new OAuthApplicationInfo();

        //Subscriber's name should be passed as a parameter, since it's under the subscriber the OAuth App is created.
        String userId = profile.getOwner();
        String applicationName = profile.getClientName();
        String grantType = profile.getGrantType();

        if (log.isDebugEnabled()) {
            log.debug("Trying to create OAuth application: '" + applicationName + "'");
        }

        String callBackURL = profile.getCallbackUrl();

        String tokenScope = profile.getTokenScope();
        String tokenScopes[] = new String[1];
        tokenScopes[0] = tokenScope;

        oAuthApplicationInfo.addParameter("tokenScope", Arrays.toString(tokenScopes));
        OAuthApplicationInfo info;
        try {
            info = createOAuthApplication(userId, applicationName, callBackURL, grantType);
        } catch (Exception e) {
            throw new APIManagementException("Can not create OAuth application  : " + applicationName, e);
        }

        if (info == null || info.getJsonString() == null) {
            throw new APIManagementException("OAuth app does not contain required data: '" + applicationName + "'");
        }

        oAuthApplicationInfo.setClientName(info.getClientName());
        oAuthApplicationInfo.setClientId(info.getClientId());
        oAuthApplicationInfo.setCallBackURL(info.getCallBackURL());
        oAuthApplicationInfo.setClientSecret(info.getClientSecret());

        try {
            JSONObject jsonObject = new JSONObject(info.getJsonString());
            if (jsonObject.has(ApplicationConstants.ClientMetadata.OAUTH_REDIRECT_URIS)) {
                oAuthApplicationInfo.addParameter(ApplicationConstants.ClientMetadata.OAUTH_REDIRECT_URIS,
                        jsonObject.get(ApplicationConstants.ClientMetadata.OAUTH_REDIRECT_URIS));
            }

            if (jsonObject.has(ApplicationConstants.ClientMetadata.OAUTH_CLIENT_GRANT)) {
                oAuthApplicationInfo.addParameter(ApplicationConstants.ClientMetadata.
                        OAUTH_CLIENT_GRANT, jsonObject.get(ApplicationConstants.ClientMetadata.OAUTH_CLIENT_GRANT));
            }
        } catch (JSONException e) {
            throw new APIManagementException("Can not retrieve information of the created OAuth application", e);
        }
        return oAuthApplicationInfo;
    }

    public static OAuthApplicationInfo createOAuthApplication(
            String userId, String applicationName, String callbackUrl, String grantType)
            throws APIManagementException, IdentityException {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        String tenantDomain = MultitenantUtils.getTenantDomain(userId);
        String baseUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String userName = MultitenantUtils.getTenantAwareUsername(userId);

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);

        // Acting as the provided user. When creating Service Provider/OAuth App,
        // username is fetched from CarbonContext
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(userName);

        try {
            // Append the username before Application name to make application name unique across two users.
            applicationName = userName + "_" + applicationName;

            // Create the Service Provider
            ServiceProvider serviceProvider = new ServiceProvider();
            serviceProvider.setApplicationName(applicationName);
            serviceProvider.setDescription("Service Provider for application " + applicationName);

            ApplicationManagementService appMgtService = ApplicationManagementService.getInstance();
            appMgtService.createApplication(serviceProvider);

            ServiceProvider createdServiceProvider = appMgtService.getApplication(applicationName);
            if (createdServiceProvider == null) {
                throw new APIManagementException("Couldn't create Service Provider Application " + applicationName);
            }

            // Then Create OAuthApp
            OAuthAdminService oAuthAdminService = new OAuthAdminService();

            OAuthConsumerAppDTO oAuthConsumerAppDTO = new OAuthConsumerAppDTO();
            oAuthConsumerAppDTO.setApplicationName(applicationName);
            oAuthConsumerAppDTO.setCallbackUrl(callbackUrl);
            oAuthConsumerAppDTO.setGrantTypes(grantType);
            if (log.isDebugEnabled()) {
                log.debug("Creating OAuth App " + applicationName);
            }

            oAuthAdminService.registerOAuthApplicationData(oAuthConsumerAppDTO);
            if (log.isDebugEnabled()) {
                log.debug("Created OAuth App " + applicationName);
            }

            OAuthConsumerAppDTO createdApp = oAuthAdminService.getOAuthApplicationDataByAppName(oAuthConsumerAppDTO
                    .getApplicationName());
            if (log.isDebugEnabled()) {
                log.debug("Retrieved Details for OAuth App " + createdApp.getApplicationName());
            }
            // Set the OAuthApp in InboundAuthenticationConfig
            InboundAuthenticationConfig inboundAuthenticationConfig = new InboundAuthenticationConfig();
            InboundAuthenticationRequestConfig[] inboundAuthenticationRequestConfigs = new
                    InboundAuthenticationRequestConfig[1];
            InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig = new
                    InboundAuthenticationRequestConfig();

            inboundAuthenticationRequestConfig.setInboundAuthKey(createdApp.getOauthConsumerKey());
            inboundAuthenticationRequestConfig.setInboundAuthType("oauth2");
            if (createdApp.getOauthConsumerSecret() != null && !createdApp.
                    getOauthConsumerSecret().isEmpty()) {
                Property property = new Property();
                property.setName("oauthConsumerSecret");
                property.setValue(createdApp.getOauthConsumerSecret());
                Property[] properties = {property};
                inboundAuthenticationRequestConfig.setProperties(properties);
            }

            inboundAuthenticationRequestConfigs[0] = inboundAuthenticationRequestConfig;
            inboundAuthenticationConfig.setInboundAuthenticationRequestConfigs(inboundAuthenticationRequestConfigs);
            createdServiceProvider.setInboundAuthenticationConfig(inboundAuthenticationConfig);

            // Update the Service Provider app to add OAuthApp as an Inbound Authentication Config
            appMgtService.updateApplication(createdServiceProvider);

            OAuthApplicationInfo oAuthApplicationInfo = new OAuthApplicationInfo();
            oAuthApplicationInfo.setClientId(createdApp.getOauthConsumerKey());
            oAuthApplicationInfo.setCallBackURL(createdApp.getCallbackUrl());
            oAuthApplicationInfo.setClientSecret(createdApp.getOauthConsumerSecret());
            oAuthApplicationInfo.setClientName(createdApp.getApplicationName());

            oAuthApplicationInfo.addParameter(
                    ApplicationConstants.ClientMetadata.OAUTH_REDIRECT_URIS, createdApp.getCallbackUrl());
            oAuthApplicationInfo.addParameter(
                    ApplicationConstants.ClientMetadata.OAUTH_CLIENT_GRANT, createdApp.getGrantTypes());
            return oAuthApplicationInfo;
        } catch (IdentityApplicationManagementException e) {
            APIUtil.handleException("Error occurred while creating ServiceProvider for app " + applicationName, e);
        } catch (Exception e) {
            APIUtil.handleException("Error occurred while creating OAuthApp " + applicationName, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(baseUser);
        }
        return null;
    }

    public static void unregisterApplication(String userId, String applicationName,
                                             String consumerKey) throws APIManagementException {
        String tenantDomain = MultitenantUtils.getTenantDomain(userId);
        String baseUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String userName = MultitenantUtils.getTenantAwareUsername(userId);

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(userName);

        if (userId == null || userId.isEmpty()) {
            throw new APIManagementException("Error occurred while unregistering Application: userId cannot " +
                    "be null/empty");
        }
        try {
            OAuthAdminService oAuthAdminService = new OAuthAdminService();
            OAuthConsumerAppDTO oAuthConsumerAppDTO = oAuthAdminService.getOAuthApplicationData(consumerKey);

            if (oAuthConsumerAppDTO == null) {
                throw new APIManagementException("Couldn't retrieve OAuth Consumer Application associated with the " +
                        "given consumer key: " + consumerKey);
            }
            oAuthAdminService.removeOAuthApplicationData(consumerKey);

            ApplicationManagementService appMgtService = ApplicationManagementService.getInstance();
            ServiceProvider createdServiceProvider = appMgtService.getApplication(applicationName);

            if (createdServiceProvider == null) {
                throw new APIManagementException("Couldn't retrieve Service Provider Application " + applicationName);
            }
            appMgtService.deleteApplication(applicationName);

        } catch (IdentityApplicationManagementException e) {
            APIUtil.handleException("Error occurred while removing ServiceProvider for app " + applicationName, e);
        } catch (Exception e) {
            APIUtil.handleException("Error occurred while removing OAuthApp " + applicationName, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(baseUser);
        }
    }

}
