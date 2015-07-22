/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.policy.mgt.common;

import org.wso2.carbon.device.mgt.common.Device;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This class will be the used to create policy object with relevant information for evaluating.
 */
@XmlRootElement
public class Policy implements Comparable<Policy>, Serializable {

    private static final long serialVersionUID = 19981017L;

    private int id;                         // Identifier of the policy.
    private int priorityId;                 // Priority of the policies. This will be used only for simple evaluation.
    private Profile profile;                  // Profile
    private String policyName;              // Name of the policy.
    private boolean generic;                // If true, this should be applied to all related device.
    private List<String> roles;          // Roles which this policy should be applied.
    private String ownershipType;           // Ownership type (COPE, BYOD, CPE)
    private List<Device> devices;        // Individual devices this policy should be applied
    private List<String> users;


    /* Compliance data*/
    private String Compliance;

    /*Dynamic policy attributes*/

    /* This is related criteria based policy */

    private List<PolicyCriterion> policyCriterias;

    /*These are related to time based policies*/

//    private int startTime;                  // Start time to apply the policy.
//    private int endTime;                    // After this time policy will not be applied
//    private Date startDate;                 // Start date to apply the policy
//    private Date endDate;                   // After this date policy will not be applied.


    /*These are related to location based policies*/

    //    private String latitude;                // Latitude
//    private String longitude;               // Longitude
//
    private int tenantId;
    private int profileId;

    /*This will be used to record attributes which will be used by customer extended PDPs and PIPs*/

    private Map<String, Object> attributes;

    @XmlElement
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @XmlElement
    public int getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(int priorityId) {
        this.priorityId = priorityId;
    }

    @XmlElement
    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @XmlElement
    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    @XmlElement
    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    @XmlElement
    public boolean isGeneric() {
        return generic;
    }

    public void setGeneric(boolean generic) {
        this.generic = generic;
    }

    @XmlElement
    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @XmlElement
    public String getOwnershipType() {
        return ownershipType;
    }

    public void setOwnershipType(String ownershipType) {
        this.ownershipType = ownershipType;
    }

    @XmlElement
    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    @XmlElement
    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    @XmlElement
    public List<PolicyCriterion> getPolicyCriterias() {
        return policyCriterias;
    }

    public void setPolicyCriterias(List<PolicyCriterion> policyCriterias) {
        this.policyCriterias = policyCriterias;
    }

    public String getCompliance() {
        return Compliance;
    }

    public void setCompliance(String compliance) {
        Compliance = compliance;
    }

    //    public int getStartTime() {
//        return startTime;
//    }
//
//    public void setStartTime(int startTime) {
//        this.startTime = startTime;
//    }
//
//    @XmlElement
//    public int getEndTime() {
//        return endTime;
//    }
//
//    public void setEndTime(int endTime) {
//        this.endTime = endTime;
//    }
//
//    @XmlElement
//    public Date getStartDate() {
//        return startDate;
//    }
//
//    public void setStartDate(Date startDate) {
//        this.startDate = startDate;
//    }
//
//    @XmlElement
//    public Date getEndDate() {
//        return endDate;
//    }
//
//    public void setEndDate(Date endDate) {
//        this.endDate = endDate;
//    }
//
//    @XmlElement
//    public String getLatitude() {
//        return latitude;
//    }
//
//    public void setLatitude(String latitude) {
//        this.latitude = latitude;
//    }
//
//    @XmlElement
//    public String getLongitude() {
//        return longitude;
//    }
//
//    public void setLongitude(String longitude) {
//        this.longitude = longitude;
//    }

    @XmlElement
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @XmlElement
    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }


 /*   static final Comparator<Policy> PRIORITY_ORDER =
            new Comparator<Policy>() {
                public int compare(Policy p1, Policy p2) {
                    int dateCmp = new Integer(p2.getId()).compareTo(new Integer(p1.getId()));
                    if (dateCmp != 0)
                        return dateCmp;

                    return (p1.getId() < p2.getId() ? -1 :
                            (p1.getId() == p2.getId() ? 0 : 1));
                }
            };*/

    @Override
    public int compareTo(Policy o) {
        if (this.priorityId == o.priorityId)
            return 0;
        else if ((this.priorityId) > o.priorityId)
            return 1;
        else
            return -1;
    }
}