/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.stockmanagement.dto;

import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.domain.sourcedestination.SourceDestinationAssignment;

import lombok.Data;

import java.util.UUID;

@Data
public class ValidSourceDestinationDto {
  private UUID id;
  private UUID programId;
  private UUID facilityTypeId;
  private Node node;
  private String name;
  private Boolean isFreeTextAllowed;

  /**
   * Create DTO from JPA model.
   *
   * @param assignment JPA model
   * @param name       facility or organization name
   * @return created DTO
   */
  public static ValidSourceDestinationDto createFrom(
      SourceDestinationAssignment assignment, String name) {
    ValidSourceDestinationDto dto = new ValidSourceDestinationDto();
    dto.setName(name);
    dto.setIsFreeTextAllowed(!assignment.getNode().isRefDataFacility());
    dto.setId(assignment.getId());
    dto.setNode(assignment.getNode());
    dto.setProgramId(assignment.getProgramId());
    dto.setFacilityTypeId(assignment.getFacilityTypeId());
    return dto;
  }
}
