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
 * Logs approval status changes.
 */
public class LoggingApprovalStatusListener implements ApplicationListener<ApprovalStatusChangedEvent> {
  
  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingApprovalStatusListener.class);
  
  @Override
  public void onApplicationEvent(ApprovalStatusChangedEvent event) {
    if (LOGGER.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Approval status changed.");
      sb.append(" User: ").append(event.getUser() != null ? event.getUser().getUsername() : "unknown");
      sb.append(", Status: ").append(event.getStatus());
      sb.append(", Items: ").append(event.getIds() != null ? event.getIds().size() : 0);
      if (event.getIds() != null && !event.getIds().isEmpty()) {
        sb.append(" [").append(event.getIds().stream().collect(Collectors.joining(","))).append("]");
      }
      LOGGER.info(sb.toString());
    }
  }

}
