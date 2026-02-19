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
import com.esri.geoportal.base.util.Val;
import com.esri.geoportal.context.AppResponse;
import com.esri.geoportal.event.ItemDeletedEvent;
import com.esri.geoportal.lib.elastic.ElasticContext;
import com.esri.geoportal.lib.elastic.request.BulkEditRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Delete one or more items.
 */
public class DeleteItemsRequest extends BulkEditRequest implements ApplicationEventPublisherAware {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DeleteItemsRequest.class);

  /** Instance variables. */
  private ApplicationEventPublisher publisher;

  /** Constructor. */
  public DeleteItemsRequest() {
    super();
    this.setResponseStatusAction("deleted");
    this.setUseHttpClient(true);
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }
    
  /**
   * Append the scroller hit to the bulk request (HTTP).
   * @param ec the Elastic context
   * @param request the request
   * @param hit the hit
   */
  protected void appendHit(ElasticContext ec, StringBuilder data, com.esri.geoportal.lib.elastic.http.util.SearchHit hit) {
    // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
    JsonObjectBuilder line1 = Json.createObjectBuilder();
    JsonObjectBuilder joBuilder = Json.createObjectBuilder()
            .add("_id",hit.getId());
    if (!ec.getIs7Plus()) {
      joBuilder =joBuilder.add("_type",ec.getActualItemIndexType());
    }
    line1.add("delete", joBuilder);
    data.append(line1.build().toString()).append("\n");
    if (ec.getUseSeparateXmlItem()) {
      JsonObjectBuilder line2 = Json.createObjectBuilder();
      line2.add("delete", Json.createObjectBuilder()
        .add("_id",hit.getId()+"_xml")
        .add("_type",ec.getXmlIndexType())
      );
      data.append(line2.build().toString()).append("\n");
    }
  }
  
  @Override
  public AppResponse execute() throws Exception {
    /*
    http://localhost:8080/geoportal/rest/metadata/deteteItems?id=68e65338e166458d8425775114487b31
    */

    setAdminOnly(false);
    setProcessMessage("DeleteItems");
    //if (true) throw new RuntimeException("DeleteItemsRequest: temporary stop");

    AppResponse response = super.execute();

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

        if (!ids.isEmpty()) {
          publisher.publishEvent(new ItemDeletedEvent(this, getUser(), ids));
        }
      } catch (Exception e) {
        LOGGER.error("Failed to publish item deleted event", e);
      }
    }

    return response;
  }
  
}
