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
package com.esri.geoportal.event;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

/**
 * Listens for Geoportal events and publishes them to AWS EventBridge.
 */
public class AwsEventBridgeListener implements ApplicationListener<GeoportalEvent> {
  
  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(AwsEventBridgeListener.class);
  
  /** Instance variables. */
  private EventBridgeClient eventBridgeClient;
  private String eventBusName;
  private String region = "us-east-1";
  private boolean enabled = false;
  
  /** Sets the event bus name. */
  public void setEventBusName(String eventBusName) {
    this.eventBusName = eventBusName;
  }

  /** Sets the AWS region. */
  public void setRegion(String region) {
    this.region = region;
  }

  /** Sets whether this listener is enabled. */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Initialize the listener.
   */
  @PostConstruct
  public void init() {
    if (enabled) {
      try {
        LOGGER.info("Initializing AwsEventBridgeListener for bus: {} in region: {}", eventBusName, region);
        this.eventBridgeClient = EventBridgeClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
      } catch (Exception e) {
        LOGGER.error("Failed to initialize AwsEventBridgeListener", e);
        // creating the client failed, so we can't really be enabled
        this.enabled = false; 
      }
    }
  }

  /**
   * Cleanup resources.
   */
  @PreDestroy
  public void destroy() {
    if (this.eventBridgeClient != null) {
      this.eventBridgeClient.close();
    }
  }

  @Override
  public void onApplicationEvent(GeoportalEvent event) {
    if (!enabled || eventBridgeClient == null) {
      return;
    }

    try {
      String detailJson = null;
      String detailType = null;

      if (event instanceof ApprovalStatusChangedEvent) {
        ApprovalStatusChangedEvent approvalEvent = (ApprovalStatusChangedEvent) event;
        detailType = "ApprovalStatusChanged";
        detailJson = buildApprovalStatusPayload(approvalEvent);

      } else if (event instanceof ItemCreatedEvent) {
        ItemCreatedEvent createEvent = (ItemCreatedEvent) event;
        detailType = "ItemCreated";
        detailJson = buildItemEventPayload("create", createEvent.getUser(), createEvent.getItemId(), createEvent.getTitle());

      } else if (event instanceof ItemUpdatedEvent) {
        ItemUpdatedEvent updateEvent = (ItemUpdatedEvent) event;
        detailType = "ItemUpdated";
        detailJson = buildItemEventPayload("update", updateEvent.getUser(), updateEvent.getItemId(), updateEvent.getTitle());

      } else if (event instanceof ItemDeletedEvent) {
        ItemDeletedEvent deleteEvent = (ItemDeletedEvent) event;
        detailType = "ItemDeleted";
        detailJson = buildDeleteEventPayload(deleteEvent);

      } else {
        LOGGER.warn("Unsupported event type: {}", event.getClass().getName());
        return;
      }

      PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
          .source("com.esri.geoportal")
          .detailType(detailType)
          .detail(detailJson)
          .eventBusName(this.eventBusName)
          .build();

      PutEventsRequest request = PutEventsRequest.builder()
          .entries(entry)
          .build();

      eventBridgeClient.putEvents(request);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Published {} event to EventBridge: {}", detailType, detailJson);
      }

    } catch (Exception e) {
      LOGGER.error("Error publishing event to EventBridge", e);
    }
  }

  /**
   * Build JSON payload for approval status events.
   */
  private String buildApprovalStatusPayload(ApprovalStatusChangedEvent event) {
    JsonObjectBuilder job = Json.createObjectBuilder();
    job.add("action", "SetApprovalStatus");
    job.add("status", event.getStatus());
    job.add("userId", event.getUser() != null ? event.getUser().getUsername() : "unknown");

    List<String> ids = event.getIds();
    if (ids != null) {
      JsonArrayBuilder jab = Json.createArrayBuilder();
      for (String id: ids) {
        jab.add(id);
      }
      job.add("ids", jab);
    }

    return job.build().toString();
  }

  /**
   * Build JSON payload for item create/update events.
   */
  private String buildItemEventPayload(String action, com.esri.geoportal.context.AppUser user,
                                        String itemId, String title) {
    JsonObjectBuilder job = Json.createObjectBuilder();
    job.add("action", action);
    job.add("itemId", itemId);
    job.add("userId", user != null ? user.getUsername() : "unknown");
    if (title != null) {
      job.add("title", title);
    }
    return job.build().toString();
  }

  /**
   * Build JSON payload for item delete events.
   */
  private String buildDeleteEventPayload(ItemDeletedEvent event) {
    JsonObjectBuilder job = Json.createObjectBuilder();
    job.add("action", "delete");
    job.add("userId", event.getUser() != null ? event.getUser().getUsername() : "unknown");

    List<String> ids = event.getItemIds();
    if (ids != null) {
      JsonArrayBuilder jab = Json.createArrayBuilder();
      for (String id: ids) {
        jab.add(id);
      }
      job.add("itemIds", jab);
    }

    return job.build().toString();
  }

}
