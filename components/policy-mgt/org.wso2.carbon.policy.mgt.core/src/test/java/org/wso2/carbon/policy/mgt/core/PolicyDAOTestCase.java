/*
*  Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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

package org.wso2.carbon.policy.mgt.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.Feature;
import org.wso2.carbon.device.mgt.common.Group;
import org.wso2.carbon.device.mgt.common.GroupManagementException;
import org.wso2.carbon.device.mgt.core.dao.*;
import org.wso2.carbon.device.mgt.core.dto.Device;
import org.wso2.carbon.device.mgt.core.dto.OwnerShip;
import org.wso2.carbon.policy.mgt.common.*;
import org.wso2.carbon.policy.mgt.core.common.DBTypes;
import org.wso2.carbon.policy.mgt.core.common.TestDBConfiguration;
import org.wso2.carbon.policy.mgt.core.common.TestDBConfigurations;
import org.wso2.carbon.policy.mgt.core.dao.PolicyManagementDAOFactory;
import org.wso2.carbon.policy.mgt.core.dao.PolicyManagerDAOException;
import org.wso2.carbon.policy.mgt.core.impl.PolicyAdministratorPointImpl;
import org.wso2.carbon.policy.mgt.core.mgt.FeatureManager;
import org.wso2.carbon.policy.mgt.core.mgt.PolicyManager;
import org.wso2.carbon.policy.mgt.core.mgt.ProfileManager;
import org.wso2.carbon.policy.mgt.core.mgt.impl.FeatureManagerImpl;
import org.wso2.carbon.policy.mgt.core.mgt.impl.PolicyManagerImpl;
import org.wso2.carbon.policy.mgt.core.mgt.impl.ProfileManagerImpl;
import org.wso2.carbon.policy.mgt.core.util.*;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class PolicyDAOTestCase {


    private static final Log log = LogFactory.getLog(PolicyDAOTestCase.class);
    private static DataSource dataSource;
    private List<Feature> featureList;
    private List<ProfileFeature> profileFeatureList;
    private Profile profile;
    private Policy policy;
    private List<Device> devices;

    @BeforeClass
    @Parameters("dbType")
    public void setUpDB(String dbTypeStr) throws Exception {
        DBTypes dbType = DBTypes.valueOf(dbTypeStr);
        TestDBConfiguration dbConfig = getTestDBConfiguration(dbType);
        PoolProperties properties = new PoolProperties();

        log.info("Database Type : " + dbTypeStr);

        switch (dbType) {

            case MySql:

                log.info("Mysql Called..................................................." + dbTypeStr);

                properties.setUrl(dbConfig.getConnectionUrl());
                properties.setDriverClassName(dbConfig.getDriverClass());
                properties.setUsername(dbConfig.getUserName());
                properties.setPassword(dbConfig.getPwd());
                dataSource = new org.apache.tomcat.jdbc.pool.DataSource(properties);
                PolicyManagementDAOFactory.init(dataSource);
                DeviceManagementDAOFactory.init(dataSource);
                break;

            case H2:

                properties.setUrl(dbConfig.getConnectionUrl());
                properties.setDriverClassName(dbConfig.getDriverClass());
                properties.setUsername(dbConfig.getUserName());
                properties.setPassword(dbConfig.getPwd());
                dataSource = new org.apache.tomcat.jdbc.pool.DataSource(properties);
                this.initH2SQLScript();
                PolicyManagementDAOFactory.init(dataSource);
                DeviceManagementDAOFactory.init(dataSource);
                break;

            default:
        }
    }

    private TestDBConfiguration getTestDBConfiguration(DBTypes dbType) throws PolicyManagerDAOException,
            PolicyManagementException {
        File deviceMgtConfig = new File("src/test/resources/testdbconfig.xml");
        Document doc;
        TestDBConfigurations dbConfigs;

        doc = PolicyManagerUtil.convertToDocument(deviceMgtConfig);
        JAXBContext testDBContext;

        try {
            testDBContext = JAXBContext.newInstance(TestDBConfigurations.class);
            Unmarshaller unmarshaller = testDBContext.createUnmarshaller();
            dbConfigs = (TestDBConfigurations) unmarshaller.unmarshal(doc);
        } catch (JAXBException e) {
            throw new PolicyManagerDAOException("Error parsing test db configurations", e);
        }
        for (TestDBConfiguration config : dbConfigs.getDbTypesList()) {
            if (config.getDbType().equals(dbType.toString())) {
                return config;
            }
        }
        return null;
    }

    private void initH2SQLScript() throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = this.getDataSource().getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate("RUNSCRIPT FROM './src/test/resources/sql/CreateH2TestDB.sql'");
        } finally {
            TestUtils.cleanupResources(conn, stmt, null);
        }
    }

    private void initMySQlSQLScript() throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = this.getDataSource().getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate("RUNSCRIPT FROM './src/test/resources/sql/CreateMySqlTestDB.sql'");
        } finally {
            TestUtils.cleanupResources(conn, stmt, null);
        }
    }

    private DataSource getDataSource() {
        return dataSource;
    }

    @Test
    public void addDeviceType() throws DeviceManagementDAOException {

        DeviceTypeDAO deviceTypeDAO = DeviceManagementDAOFactory.getDeviceTypeDAO();
        deviceTypeDAO.addDeviceType(DeviceTypeCreator.getDeviceType());
    }


    @Test
    public void addGroupTest() throws GroupManagementDAOException, GroupManagementException {
        GroupDAO groupMgtDAO = DeviceManagementDAOFactory.getGroupDAO();

        Group group = new Group();
        group.setName("Test Group");
        group.setDateOfCreation(new Date().getTime());
        group.setDateOfLastUpdate(new Date().getTime());
        group.setDescription("test group description");
        group.setOwnerShip(OwnerShip.BYOD.toString());
        group.setOwnerId("111");
        group.setTenantId(-1234);
        groupMgtDAO.addGroup(group);
    }

    private int getGroupId() throws GroupManagementDAOException {
        GroupDAO groupMgtDAO = DeviceManagementDAOFactory.getGroupDAO();
        List<Group> groupList = groupMgtDAO.getGroups();
        if (!groupList.isEmpty()) {
            return groupList.get(0).getId();
        }
        return -1;
    }

    @Test(dependsOnMethods = {"addDeviceType", "addGroupTest"})
    public void addDevice() throws DeviceManagementDAOException, GroupManagementDAOException {

        DeviceDAO deviceTypeDAO = DeviceManagementDAOFactory.getDeviceDAO();
        devices = DeviceCreator.getDeviceList(DeviceTypeCreator.getDeviceType());
        for (Device device : devices) {
            device.setGroupId(getGroupId());
            deviceTypeDAO.addDevice(device);
        }
    }


    @Test(dependsOnMethods = ("addDevice"))
    public void addFeatures() throws FeatureManagementException {

        FeatureManager featureManager = new FeatureManagerImpl();
        featureList = FeatureCreator.getFeatureList();
        //featureManager.addFeatures(featureList);
        for (Feature feature : featureList) {
//            featureManager.addFeature(feature);
        }

    }

    @Test(dependsOnMethods = ("addFeatures"))
    public void addProfileFeatures() throws ProfileManagementException {

        ProfileManager profileManager = new ProfileManagerImpl();
        profile = ProfileCreator.getProfile(featureList);
        profileManager.addProfile(profile);
        profileFeatureList = profile.getProfileFeaturesList();
    }

    @Test(dependsOnMethods = ("addProfileFeatures"))
    public void addPolicy() throws PolicyManagementException {

        PolicyManager policyManager = new PolicyManagerImpl();
        policy = PolicyCreator.createPolicy(profile);
        policyManager.addPolicy(policy);
    }

    @Test(dependsOnMethods = ("addPolicy"))
    public void addPolicyToRole() throws PolicyManagementException {
        PolicyManager policyManager = new PolicyManagerImpl();

        List<String> roles = new ArrayList<String>();
        roles.add("Test_ROLE_01");
        roles.add("Test_ROLE_02");
        roles.add("Test_ROLE_03");

        policyManager.addPolicyToRole(roles, policy);

    }

    @Test(dependsOnMethods = ("addPolicyToRole"))
    public void addPolicyToDevice() throws PolicyManagementException {
        PolicyManager policyManager = new PolicyManagerImpl();
        Device device = DeviceCreator.getSingleDevice();

        List<DeviceIdentifier> deviceIdentifierList = new ArrayList<DeviceIdentifier>();
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
        deviceIdentifier.setId(device.getDeviceIdentificationId());
        deviceIdentifier.setType("android");

        deviceIdentifierList.add(deviceIdentifier);
        policyManager.addPolicyToDevice(deviceIdentifierList, policy);

    }

    @Test(dependsOnMethods = ("addPolicyToDevice"))
    public void addNewPolicy() throws PolicyManagementException {

        PolicyManager policyManager = new PolicyManagerImpl();
        policy = PolicyCreator.createPolicy2(profile);
        policyManager.addPolicy(policy);
    }


    @Test(dependsOnMethods = ("addPolicyToDevice"))
    public void addThirdPolicy() throws PolicyManagementException {

        PolicyManager policyManager = new PolicyManagerImpl();
        policy = PolicyCreator.createPolicy4(profile);
        policyManager.addPolicy(policy);
    }

    @Test(dependsOnMethods = ("addNewPolicy"))
    public void getPolicies() throws PolicyManagementException {
        PolicyAdministratorPoint policyAdministratorPoint = new PolicyAdministratorPointImpl();
        List<Policy> policyList = policyAdministratorPoint.getPolicies();

        log.debug("----------All policies---------");

        for (Policy policy : policyList) {
            log.debug("Policy Id : " + policy.getId() + " Policy Name : " + policy.getPolicyName());
            log.debug("Policy Ownership type :" + policy.getOwnershipType());

            List<String> users = policy.getUsers();
            for (String user : users) {
                log.debug("User of the policy : " + user);
            }

            List<String> roles = policy.getRoles();
            for (String role : roles) {
                log.debug("User of the policy : " + role);
            }
        }
    }

    @Test(dependsOnMethods = ("getPolicies"))
    public void getDeviceTypeRelatedPolicy() throws PolicyManagementException {

        PolicyAdministratorPoint policyAdministratorPoint = new PolicyAdministratorPointImpl();
        List<Policy> policyList = policyAdministratorPoint.getPoliciesOfDeviceType("android");

        log.debug("----------Device type related policy---------");

        for (Policy policy : policyList) {
            log.debug("Policy Id : " + policy.getId() + " Policy Name : " + policy.getPolicyName());
            log.debug("Policy Ownership type :" + policy.getOwnershipType());

            List<String> users = policy.getUsers();
            for (String user : users) {
                log.debug("User of the policy : " + user);
            }

            List<String> roles = policy.getRoles();
            for (String role : roles) {
                log.debug("User of the policy : " + role);
            }
        }
    }


    @Test(dependsOnMethods = ("getDeviceTypeRelatedPolicy"))
    public void getUserRelatedPolicy() throws PolicyManagementException {

        PolicyAdministratorPoint policyAdministratorPoint = new PolicyAdministratorPointImpl();
        List<Policy> policyList = policyAdministratorPoint.getPoliciesOfUser("Dilshan");

        log.debug("----------User related policy---------");

        for (Policy policy : policyList) {
            log.debug("Policy Id : " + policy.getId() + " Policy Name : " + policy.getPolicyName());
            log.debug("Policy Ownership type :" + policy.getOwnershipType());

            List<String> users = policy.getUsers();
            for (String user : users) {
                log.debug("User of the policy : " + user);
            }

            List<String> roles = policy.getRoles();
            for (String role : roles) {
                log.debug("User of the policy : " + role);
            }
        }
    }

    @Test(dependsOnMethods = ("getDeviceTypeRelatedPolicy"))
    public void getRoleRelatedPolicy() throws PolicyManagementException {

        PolicyAdministratorPoint policyAdministratorPoint = new PolicyAdministratorPointImpl();
        List<Policy> policyList = policyAdministratorPoint.getPoliciesOfRole("Test_ROLE_01");

        log.debug("----------Roles related policy---------");

        for (Policy policy : policyList) {
            log.debug("Policy Id : " + policy.getId() + " Policy Name : " + policy.getPolicyName());

            log.debug("Policy Ownership type :" + policy.getOwnershipType());
        }
    }

    @Test(dependsOnMethods = ("getRoleRelatedPolicy"))
    public void addSecondPolicy() throws PolicyManagementException {
        PolicyManager policyManager = new PolicyManagerImpl();
        policy = PolicyCreator.createPolicy3(profile);
        policyManager.addPolicy(policy);
    }

    @Test(dependsOnMethods = ("getDeviceTypeRelatedPolicy"))
    public void getRoleRelatedPolicySecondTime() throws PolicyManagementException {

        PolicyAdministratorPoint policyAdministratorPoint = new PolicyAdministratorPointImpl();
        List<Policy> policyList = policyAdministratorPoint.getPoliciesOfRole("Role_01");

        log.debug("----------Roles related policy second time ---------");

        for (Policy policy : policyList) {
            log.debug("Policy Id : " + policy.getId() + " Policy Name : " + policy.getPolicyName());

            log.debug("Policy Ownership type :" + policy.getOwnershipType());

            List<ProfileFeature> profileFeatures = policy.getProfile().getProfileFeaturesList();

            for (ProfileFeature profileFeature : profileFeatures) {
                log.debug("Feature Content" + profileFeature.getId() + " - " + profileFeature.getContent());
            }

        }
    }

    @Test(dependsOnMethods = ("getRoleRelatedPolicySecondTime"))
    public void getRoleRelatedPolicyThirdTime() throws PolicyManagementException {

        PolicyAdministratorPoint policyAdministratorPoint = new PolicyAdministratorPointImpl();
        List<Policy> policyList = policyAdministratorPoint.getPoliciesOfRole("Role_02");


        log.debug("----------Roles related policy third time ---------");

        for (Policy policy : policyList) {
            log.debug("Policy Id : " + policy.getId() + " Policy Name : " + policy.getPolicyName());

            List<ProfileFeature> profileFeatures = policy.getProfile().getProfileFeaturesList();

//            for (ProfileFeature profileFeature : profileFeatures) {
//                log.debug("Feature Content" + profileFeature.getId() + " - " + profileFeature.getContent());
//            }

            List<PolicyCriterion> criteria = policy.getPolicyCriterias();

            for (PolicyCriterion criterion : criteria) {
                log.debug("Criterias " + criterion.getName() + " -- " + criterion.getCriteriaId() + " -- "
                        + criterion.getId());

                Properties prop = criterion.getProperties();

                for (String key : prop.stringPropertyNames()) {
                    log.debug("Property Names : " + key + " -- " + prop.getProperty(key));
                }
            }

        }
    }


    @Test(dependsOnMethods = ("getRoleRelatedPolicyThirdTime"))
    public void deletPolicy() throws PolicyManagementException {
        PolicyAdministratorPoint policyAdministratorPoint = new PolicyAdministratorPointImpl();
        policyAdministratorPoint.deletePolicy(1);

        log.debug("First policy deleted.");
    }


}
