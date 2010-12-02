/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 * 
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ExtractResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.InternalErrorException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@Implementation(description="Extracts a template", responseObject=ExtractResponse.class)
public class ExtractTemplateCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(ExtractTemplateCmd.class.getName());

    private static final String s_name = "extracttemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the template")
    private Long id;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=false, description="the url to which the ISO would be extracted")
    private String url;
    
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the ID of the zone where the ISO is originally located" )
    private Long zoneId;

    @Parameter(name=ApiConstants.MODE, type=CommandType.STRING, required=true, description="the mode of extraction - HTTP_DOWNLOAD or FTP_UPLOAD")
    private String mode;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getMode() {
        return mode;
    }
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

	@Override
	public String getName() {
		return s_name;
	}
	
    public static String getStaticName() {
        return s_name;
    }

    @Override
    public long getAccountId() {
        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, getId());
        if (template != null) {
            return template.getAccountId();
        }

        // invalid id, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TEMPLATE_EXTRACT;
    }

    @Override
    public String getEventDescription() {
        return  "Extraction job";
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Template;
    }
    
    public Long getInstanceId() {
    	return getId();
    }
	
    @Override
    public void execute(){
        try {
            Long uploadId = _templateService.extract(this);
            if (uploadId != null){
                ExtractResponse response = _responseGenerator.createExtractResponse(uploadId, id, zoneId, getAccountId(), mode);
                response.setResponseName(getName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to extract template");
            }
        } catch (InternalErrorException ex) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
