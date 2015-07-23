/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.carbon.device.mgt.core.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.internal.CarbonContextDataHolder;
import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.core.DeviceManagementPluginRepository;
import org.wso2.carbon.device.mgt.core.TestDeviceManagementService;
import org.wso2.carbon.device.mgt.core.common.BaseDeviceManagementTest;
import org.wso2.carbon.device.mgt.core.common.TestDataHolder;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOException;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOFactory;
import org.wso2.carbon.device.mgt.core.util.DeviceManagerUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class DeviceManagementProviderServiceTest extends BaseDeviceManagementTest {

    private static final Log log = LogFactory.getLog(DeviceManagementProviderServiceTest.class);
    DeviceManagementProviderService deviceManagementProviderService = null;


    @BeforeClass
    @Override
    public void init() throws Exception {
        initDatSource();
    }

  @Test
    public void testEnrollment() {

        try {
            DeviceManagementPluginRepository deviceManagementPluginRepository = new DeviceManagementPluginRepository();
            TestDeviceManagementService testDeviceManagementService = new TestDeviceManagementService(TestDataHolder.TEST_DEVICE_TYPE);
            deviceManagementPluginRepository.addDeviceManagementProvider(testDeviceManagementService);

            deviceManagementProviderService = new DeviceManagementProviderServiceImpl(deviceManagementPluginRepository,
                    true);
            DeviceManagerUtil.registerDeviceType(TestDataHolder.TEST_DEVICE_TYPE);

            Device device = TestDataHolder.generateDummyDeviceData(TestDataHolder.TEST_DEVICE_TYPE);
            boolean isEnrolled = deviceManagementProviderService.enrollDevice(device);

            Assert.assertEquals(isEnrolled,true,"Enrolment fail");
            if (isEnrolled){
                TestDataHolder.initialTestDevice = device;
            }

        } catch (DeviceManagementException e) {
            String msg = "Error occurred while adding device type '" + TestDataHolder.TEST_DEVICE_TYPE + "'";
            log.error(msg, e);
            Assert.fail(msg, e);
        } finally {
            try {
                DeviceManagementDAOFactory.closeConnection();
            } catch (DeviceManagementDAOException e) {
                log.warn("Error occurred while closing the connection", e);
            }
        }
    }

    @AfterClass
    public void cleanResources(){
    }
}
