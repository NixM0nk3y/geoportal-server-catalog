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

import com.esri.geoportal.context.AppUser;

/**
 * Event published when a new item is created.
 */
public class ItemCreatedEvent extends GeoportalEvent {

  private AppUser user;
  private String itemId;
  private String title;

  /**
   * Constructor.
   * @param source the source component
   * @param user the user who created the item
   * @param itemId the ID of the created item
   * @param title the title of the item (may be null)
   */
  public ItemCreatedEvent(Object source, AppUser user, String itemId, String title) {
    super(source);
    this.user = user;
    this.itemId = itemId;
    this.title = title;
  }

  /** The user who created the item. */
  public AppUser getUser() {
    return user;
  }

  /** The ID of the created item. */
  public String getItemId() {
    return itemId;
  }

  /** The title of the item. */
  public String getTitle() {
    return title;
  }

}
