package org.wso2.carbon.device.mgt.group.core.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.core.util.DeviceManagerUtil;
import org.wso2.carbon.device.mgt.group.common.Group;
import org.wso2.carbon.device.mgt.group.common.GroupManagementException;
import org.wso2.carbon.device.mgt.group.core.dao.GroupDAO;
import org.wso2.carbon.device.mgt.group.core.dao.GroupManagementDAOException;
import org.wso2.carbon.device.mgt.group.core.dao.GroupManagementDAOFactory;
import org.wso2.carbon.device.mgt.group.core.internal.DeviceMgtGroupDataHolder;
import org.wso2.carbon.device.mgt.user.common.User;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.Permission;
import org.wso2.carbon.user.core.UserCoreConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupManagementServiceProviderImpl implements GroupManagementServiceProvider {

    private static Log log = LogFactory.getLog(GroupManagementServiceProviderImpl.class);

    public static final String GIVEN_NAME = UserCoreConstants.ClaimTypeURIs.GIVEN_NAME;
    public static final String EMAIL_ADDRESS = UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS;
    public static final String SURNAME = UserCoreConstants.ClaimTypeURIs.SURNAME;
    public static final String STREET_ADDRESS = UserCoreConstants.ClaimTypeURIs.STREET_ADDRESS;
    public static final String LOCALITY = UserCoreConstants.ClaimTypeURIs.LOCALITY;
    public static final String REGION = UserCoreConstants.ClaimTypeURIs.REGION;
    public static final String POSTAL_CODE = UserCoreConstants.ClaimTypeURIs.POSTAL_CODE;
    public static final String COUNTRY = UserCoreConstants.ClaimTypeURIs.COUNTRY;
    public static final String HONE = UserCoreConstants.ClaimTypeURIs.HONE;
    public static final String IM = UserCoreConstants.ClaimTypeURIs.IM;
    public static final String ORGANIZATION = UserCoreConstants.ClaimTypeURIs.ORGANIZATION;
    public static final String URL = UserCoreConstants.ClaimTypeURIs.URL;
    public static final String TITLE = UserCoreConstants.ClaimTypeURIs.TITLE;
    public static final String ROLE = UserCoreConstants.ClaimTypeURIs.ROLE;
    public static final String MOBILE = UserCoreConstants.ClaimTypeURIs.MOBILE;
    public static final String NICKNAME = UserCoreConstants.ClaimTypeURIs.NICKNAME;
    public static final String DATE_OF_BIRTH = UserCoreConstants.ClaimTypeURIs.DATE_OF_BIRTH;
    public static final String GENDER = UserCoreConstants.ClaimTypeURIs.GENDER;
    public static final String ACCOUNT_STATUS = UserCoreConstants.ClaimTypeURIs.ACCOUNT_STATUS;
    public static final String CHALLENGE_QUESTION_URI = UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI;
    public static final String IDENTITY_CLAIM_URI = UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI;
    public static final String TEMPORARY_EMAIL_ADDRESS = UserCoreConstants.ClaimTypeURIs.TEMPORARY_EMAIL_ADDRESS;

    private GroupDAO groupDAO;

    public GroupManagementServiceProviderImpl() {
        this.groupDAO = GroupManagementDAOFactory.getGroupDAO();
    }

    @Override
    public boolean createGroup(Group group) throws GroupManagementException {
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            group.setTenantId(tenantId);
            this.groupDAO.addGroup(group);
            int groupId = this.groupDAO.getGroupByName(group.getName(), group.getTenantId()).getId();
            if (groupId == 0) {
                return false;
            }
            group.setId(groupId);
            addNewSharingRoleForGroup(group.getOwnerId(), group.getId(), "admin", null);
            addNewSharingRoleForGroup(group.getOwnerId(), group.getId(), "monitor", null);
            addNewSharingRoleForGroup(group.getOwnerId(), group.getId(), "operator", null);
            log.info("Group added: " + group.getName());
            return true;
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while adding group " +
                    "'" + group.getName() + "'", e);
        } catch (GroupManagementException e) {
            throw new GroupManagementException("Error occurred while adding group " +
                    "'" + group.getName() + "' role to user " + group.getOwnerId(), e);
        }
    }

    @Override
    public void updateGroup(Group group) throws GroupManagementException {
        try {
            this.groupDAO.updateGroup(group);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while modifying group " +
                    "'" + group.getName() + "'", e);
        }
    }

    @Override
    public boolean deleteGroup(int groupId) throws GroupManagementException {
        String roleName;
        try {
            Group group = getGroupById(groupId);
            if (group == null) {
                return false;
            }
            List<String> groupRoles = getAllRolesForGroup(groupId);
            for (String role : groupRoles) {
                if (role != null) {
                    roleName = role.replace("Internal/groups/" + groupId + "/", "");
                    removeSharingRoleForGroup(groupId, roleName);
                }
            }
            this.groupDAO.deleteGroup(groupId);
            log.info("Group removed: " + group.getName());
            return true;
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while removing group " +
                    "'" + groupId + "' data", e);
        } catch (GroupManagementException e) {
            throw new GroupManagementException("Error occurred while removing group " +
                    "'" + groupId + "' roles", e);
        }
    }

    @Override
    public Group getGroupById(int groupId) throws GroupManagementException {
        try {
            return this.groupDAO.getGroupById(groupId);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while obtaining group " + groupId, e);
        }
    }

    @Override
    public Group getGroupByName(String groupName) throws GroupManagementException {
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            return this.groupDAO.getGroupByName(groupName, tenantId);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while obtaining group " + groupName, e);
        }
    }

    @Override
    public List<Group> getGroupListOfUser(String username) throws GroupManagementException {
        UserStoreManager userStoreManager;
        List<Group> groupList = new ArrayList<Group>();
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = DeviceMgtGroupDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            String[] roleList = userStoreManager.getRoleListOfUser(username);
            for (String role : roleList) {
                if (role != null && role.contains("Internal/groups/")) {
                    int groupId = Integer.parseInt(role.split("/")[2]);
                    Group group = getGroupById(groupId);
                    groupList.add(group);
                }
            }
            return groupList;
        } catch (UserStoreException e) {
            throw new GroupManagementException("Error occurred while getting user store manager", e);
        }
    }

    @Override
    public int getGroupCountOfUser(String username) throws GroupManagementException {
        int tenantId = DeviceManagerUtil.getTenantId();
        try {
            return this.groupDAO.getGroupCountOfUser(username, tenantId);
        } catch (GroupManagementDAOException e) {
            throw new GroupManagementException("Error occurred while getting group count", e);
        }
    }

    @Override
    public boolean shareGroup(String username, int groupId, String sharingRole) throws GroupManagementException {
        UserStoreManager userStoreManager;
        String[] roles = new String[1];
        try {
            Group group = getGroupById(groupId);
            if (group == null) {
                return false;
            }
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = DeviceMgtGroupDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            roles[0] = "Internal/groups/" + groupId + "/" + sharingRole;
            userStoreManager.updateRoleListOfUser(username, null, roles);
            return true;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding user " + username + " to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override
    public boolean unShareGroup(String username, int groupId, String sharingRole) throws GroupManagementException {
        UserStoreManager userStoreManager;
        String[] roles = new String[1];
        try {
            Group group = getGroupById(groupId);
            if (group == null) {
                return false;
            }
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = DeviceMgtGroupDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            roles[0] = "Internal/groups/" + groupId + "/" + sharingRole;
            userStoreManager.updateRoleListOfUser(username, roles, null);
            return true;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding user " + username + " to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override
    public boolean addNewSharingRoleForGroup(String username, int groupId, String roleName, Permission[] permissions) throws GroupManagementException {
        UserStoreManager userStoreManager;
        String role;
        String[] userNames = new String[1];
        try {
            Group group = getGroupById(groupId);
            if (group == null) {
                return false;
            }
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = DeviceMgtGroupDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            role = "Internal//groups/" + groupId + "/" + roleName;
            userNames[0] = username;
            userStoreManager.addRole(role, userNames, permissions);
            return true;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding role to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override
    public boolean removeSharingRoleForGroup(int groupId, String roleName) throws GroupManagementException {
        UserStoreManager userStoreManager;
        String role;
        try {
            Group group = getGroupById(groupId);
            if (group == null) {
                return false;
            }
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = DeviceMgtGroupDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            role = "Internal/groups/" + groupId + "/" + roleName;
            userStoreManager.deleteRole(role);
            return true;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding role to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override
    public List<String> getAllRolesForGroup(int groupId) throws GroupManagementException {
        UserStoreManager userStoreManager;
        String[] roles;
        List<String> groupRoles;
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = DeviceMgtGroupDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            roles = userStoreManager.getRoleNames();
            groupRoles = new ArrayList<String>();
            for (String r : roles) {
                if (r != null && r.contains("groups/" + groupId)) {
                    groupRoles.add(r);
                }
            }
            return groupRoles;
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in adding role to group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
    }

    @Override
    public List<String> getGroupRolesForUser(String username, int groupId) throws GroupManagementException {
        UserStoreManager userStoreManager;
        List<String> groupRoleList = new ArrayList<String>();
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = DeviceMgtGroupDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            String[] roleList = userStoreManager.getRoleListOfUser(username);
            for (String role : roleList) {
                if (role != null && role.contains("Internal/groups/" + groupId)) {
                    String roleName = role.replace("Internal/groups/" + groupId + "/", "");
                    groupRoleList.add(roleName);
                }
            }
            return groupRoleList;
        } catch (UserStoreException e) {
            throw new GroupManagementException("Error occurred while getting user store manager", e);
        }
    }

    @Override
    public List<User> getUsersForGroup(int groupId) throws GroupManagementException {
        UserStoreManager userStoreManager;
        String[] userNames;
        ArrayList usersList = new ArrayList();
        try {
            int tenantId = DeviceManagerUtil.getTenantId();
            userStoreManager = DeviceMgtGroupDataHolder.getInstance().getRealmService().getTenantUserRealm(tenantId)
                    .getUserStoreManager();
            userNames = userStoreManager.getUserListOfRole("Internal/groups/" + groupId + "/*");
            User newUser;
            for (String userName : userNames) {
                newUser = new User(userName);
                Claim[] claims = userStoreManager.getUserClaimValues(userName, null);
                Map<String, String> claimMap = new HashMap<String, String>();
                for (Claim claim : claims) {
                    String claimURI = claim.getClaimUri();
                    String value = claim.getValue();
                    claimMap.put(claimURI, value);
                }
                setUserClaims(newUser, claimMap);
                usersList.add(newUser);
            }
        } catch (UserStoreException userStoreEx) {
            String errorMsg = "User store error in fetching user list for group id:" + groupId;
            log.error(errorMsg, userStoreEx);
            throw new GroupManagementException(errorMsg, userStoreEx);
        }
        return usersList;
    }

    @Override
    public List<Device> getAllDevicesInGroup(int groupId) throws GroupManagementException {
        List<Device> devicesInGroup;
        try {
            devicesInGroup = DeviceMgtGroupDataHolder.getInstance().getDeviceManagementService().getDevicesByGroup(groupId);
            return devicesInGroup;
        } catch (DeviceManagementException e) {
            throw new GroupManagementException("Error occurred while getting devices in group", e);
        }
    }

    @Override
    public boolean addDeviceToGroup(DeviceIdentifier deviceId, int groupId) throws GroupManagementException {
        Device device;
        Group group;
        try {
            device = DeviceMgtGroupDataHolder.getInstance().getDeviceManagementService().getDevice(deviceId);
            group = this.getGroupById(groupId);
            if (device == null || group == null) {
                return false;
            }
            device.setGroupId(group.getId());
            DeviceMgtGroupDataHolder.getInstance().getDeviceManagementService().modifyEnrollment(device);
        } catch (DeviceManagementException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void setUserClaims(User newUser, Map<String, String> claimMap) {
        newUser.setRoleName(UserCoreConstants.ClaimTypeURIs.ROLE);
        newUser.setAccountStatus(claimMap.get(ACCOUNT_STATUS));
        newUser.setChallengeQuestion(claimMap.get(CHALLENGE_QUESTION_URI));
        newUser.setCountry(claimMap.get(COUNTRY));
        newUser.setDateOfBirth(claimMap.get(DATE_OF_BIRTH));
        newUser.setEmail(claimMap.get(EMAIL_ADDRESS));
        newUser.setFirstName(claimMap.get(GIVEN_NAME));
        newUser.setGender(claimMap.get(GENDER));
        newUser.setHone(claimMap.get(HONE));
        newUser.setIm(claimMap.get(IM));
        newUser.setIdentityClaimUri(claimMap.get(IDENTITY_CLAIM_URI));
        newUser.setLastName(claimMap.get(SURNAME));
        newUser.setLocality(claimMap.get(LOCALITY));
        newUser.setEmail(claimMap.get(EMAIL_ADDRESS));
        newUser.setMobile(claimMap.get(MOBILE));
        newUser.setNickName(claimMap.get(NICKNAME));
        newUser.setOrganization(claimMap.get(ORGANIZATION));
        newUser.setPostalCode(claimMap.get(POSTAL_CODE));
        newUser.setRegion(claimMap.get(REGION));
        newUser.setStreatAddress(claimMap.get(STREET_ADDRESS));
        newUser.setTitle(claimMap.get(TITLE));
        newUser.setTempEmailAddress(claimMap.get(TEMPORARY_EMAIL_ADDRESS));
    }
}
