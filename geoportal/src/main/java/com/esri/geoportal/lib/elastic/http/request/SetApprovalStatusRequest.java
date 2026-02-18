/* See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Esri Inc. licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esri.geoportal.lib.elastic.http.request;
import com.esri.geoportal.base.util.JsonUtil;
import com.esri.geoportal.base.util.Val;
import com.esri.geoportal.context.AppResponse;
import com.esri.geoportal.context.GeoportalContext;
import com.esri.geoportal.event.ApprovalStatusChangedEvent;
import com.esri.geoportal.lib.elastic.request.BulkEditRequest;
import com.esri.geoportal.lib.elastic.util.FieldNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Set the approval status for one or more items.
 */
public class SetApprovalStatusRequest extends BulkEditRequest implements ApplicationEventPublisherAware {
  
  private ApplicationEventPublisher publisher;

  /** Constructor. */
  public SetApprovalStatusRequest() {
    super();
    this.setUseHttpClient(true);
  }
  
  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }
  
  @Override
  public AppResponse execute() throws Exception {
    /*
    http://localhost:8080/geoportal/rest/metadata/setApprovalStatus?id=68e65338e166458d8425775114487b31&approvalStatus=draft
    
    approvalStatus=
    */
    
    setAdminOnly(false);
    setProcessMessage("SetApprovalStatus");
    AppResponse response = new AppResponse();
    if (!GeoportalContext.getInstance().getSupportsApprovalStatus()) {
      String msg = "Not implemented";
      response.writeNotImplemented(this,JsonUtil.newErrorResponse(msg,getPretty()));
      return response;
    }
    
    String status = getParameter("approvalStatus");
    if (status != null) status = status.trim();
    if (status == null || status.length() == 0) {
      response.writeMissingParameter(this,"approvalStatus");
      return response;
    }
    status = status.toLowerCase();
    if (!status.equals("approved") && !status.equals("reviewed") &&
        !status.equals("disapproved") && !status.equals("incomplete") && 
        !status.equals("posted") && !status.equals("draft") && 
        !status.equals("active") && !status.equals("inactive")){
      String msg = "approvalStatus must be approved, reviewed, disapproved, incomplete, posted, draft, active or inactive";
      response.writeBadRequest(this,JsonUtil.newErrorResponse(msg,getPretty()));
      return response;
    }
    if (status.equals("approved") || status.equals("reviewed") || status.equals("disapproved")) {
      setAdminOnly(true);
    }
    
    JsonObjectBuilder jso = Json.createObjectBuilder();
    jso.add(FieldNames.FIELD_SYS_APPROVAL_STATUS,status);
    //jso.add(FieldNames.FIELD_SYS_MODIFIED,DateUtil.nowAsString()); // TODO should this be set?
    setUpdateSource(jso.build().toString());
    
    //System.err.println("updateSource="+this.getUpdateSource());
    //if (true) throw new RuntimeException("SetApprovalStatusRequest: temporary stop");
    
    response = super.execute();
    
    if (response.getStatus() == Response.Status.OK && publisher != null) {
      try {
        List<String> ids = new ArrayList<>();
        String[] values = getParameterValues("id");
        if (values != null && values.length == 1) {
          values = Val.tokenize(values[0],",",false);
        }
        if (values != null) {
          ids.addAll(Arrays.asList(values));
        }
        
        publisher.publishEvent(new ApprovalStatusChangedEvent(this, getUser(), status, ids));
      } catch (Exception e) {
        // Log error but don't fail the request
        e.printStackTrace();
      }
    }
    
    return response;
  }
  
}
