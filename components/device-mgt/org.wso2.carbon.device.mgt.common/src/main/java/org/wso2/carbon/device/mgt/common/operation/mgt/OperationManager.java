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
package org.wso2.carbon.device.mgt.common.operation.mgt;

import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;

import java.util.List;

/**
 * This represents the Device Operation management functionality which should be implemented by
 * the device type plugins.
 */
public interface OperationManager {

    /**
     * Method to add a operation to a device or a set of devices.
     *
     * @param operation Operation to be added
     * @param devices   List of DeviceIdentifiers to execute the operation
     * @throws OperationManagementException If some unusual behaviour is observed while adding the
     * operation
     */
    public int addOperation(Operation operation, List<DeviceIdentifier> devices) throws OperationManagementException;

    /**
     * Method to retrieve the list of all operations to a device.
     *
     * @param deviceId DeviceIdentifier of the device
     * @throws OperationManagementException If some unusual behaviour is observed while fetching the
     * operation list.
     */
    public List<? extends Operation> getOperations(DeviceIdentifier deviceId) throws OperationManagementException;

    /**
     * Method to retrieve the list of available operations to a device.
     *
     * @param deviceId DeviceIdentifier of the device
     * @throws OperationManagementException If some unusual behaviour is observed while fetching the
     * operation list.
     */
    public List<? extends Operation> getPendingOperations(
            DeviceIdentifier deviceId) throws OperationManagementException;

    public Operation getNextPendingOperation(DeviceIdentifier deviceId) throws OperationManagementException;

    public void updateOperation(DeviceIdentifier deviceId, Operation operation) throws OperationManagementException;

    public void deleteOperation(int operationId) throws OperationManagementException;

    public Operation getOperationByDeviceAndOperationId(DeviceIdentifier deviceId, int operationId)
            throws OperationManagementException;

    public List<? extends Operation> getOperationsByDeviceAndStatus(DeviceIdentifier identifier,
            Operation.Status status) throws OperationManagementException, DeviceManagementException;

    public Operation getOperation(int operationId) throws OperationManagementException;

}