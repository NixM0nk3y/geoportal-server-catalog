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

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

/**
 * Logs item lifecycle events (create, update, delete).
 */
public class LoggingItemEventListener implements ApplicationListener<GeoportalEvent> {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingItemEventListener.class);

  @Override
  public void onApplicationEvent(GeoportalEvent event) {
    if (!LOGGER.isInfoEnabled()) {
      return;
    }

    StringBuilder sb = new StringBuilder();

    if (event instanceof ItemCreatedEvent) {
      ItemCreatedEvent createEvent = (ItemCreatedEvent) event;
      sb.append("Item created.");
      sb.append(" User: ").append(createEvent.getUser() != null ? createEvent.getUser().getUsername() : "unknown");
      sb.append(", ItemId: ").append(createEvent.getItemId());
      if (createEvent.getTitle() != null) {
        sb.append(", Title: ").append(createEvent.getTitle());
      }
      LOGGER.info(sb.toString());

    } else if (event instanceof ItemUpdatedEvent) {
      ItemUpdatedEvent updateEvent = (ItemUpdatedEvent) event;
      sb.append("Item updated.");
      sb.append(" User: ").append(updateEvent.getUser() != null ? updateEvent.getUser().getUsername() : "unknown");
      sb.append(", ItemId: ").append(updateEvent.getItemId());
      if (updateEvent.getTitle() != null) {
        sb.append(", Title: ").append(updateEvent.getTitle());
      }
      LOGGER.info(sb.toString());

    } else if (event instanceof ItemDeletedEvent) {
      ItemDeletedEvent deleteEvent = (ItemDeletedEvent) event;
      sb.append("Item(s) deleted.");
      sb.append(" User: ").append(deleteEvent.getUser() != null ? deleteEvent.getUser().getUsername() : "unknown");
      sb.append(", Items: ").append(deleteEvent.getItemIds() != null ? deleteEvent.getItemIds().size() : 0);
      if (deleteEvent.getItemIds() != null && !deleteEvent.getItemIds().isEmpty()) {
        sb.append(" [").append(deleteEvent.getItemIds().stream().collect(Collectors.joining(","))).append("]");
      }
      LOGGER.info(sb.toString());
    }
  }

}
