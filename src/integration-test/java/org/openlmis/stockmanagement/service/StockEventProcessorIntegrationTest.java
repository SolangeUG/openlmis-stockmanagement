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

package org.openlmis.stockmanagement.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.stockmanagement.testutils.StockEventDtoDataBuilder.createStockEventDto;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openlmis.stockmanagement.BaseIntegrationTest;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.repository.StockEventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StockEventProcessorIntegrationTest extends BaseIntegrationTest {

  @MockBean
  private StockEventValidationsService stockEventValidationsService;

  @MockBean
  private PhysicalInventoryService physicalInventoryService;
  
  @MockBean StockEventNotificationProcessor stockEventNotificationProcessor;
  
  @Autowired
  private StockEventProcessor stockEventProcessor;

  @Autowired
  private StockEventsRepository stockEventsRepository;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private StockCardLineItemRepository lineItemRepository;

  @Autowired
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private OAuth2Authentication authentication;

  private UUID userId = UUID.randomUUID();

  private long cardSize;
  private long eventSize;
  private long lineItemSize;

  @Before
  public void setUp() throws Exception {
    cardSize = stockCardRepository.count();
    eventSize = stockEventsRepository.count();
    lineItemSize = lineItemRepository.count();

    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isClientOnly()).thenReturn(true);
  }

  @After
  public void tearDown() throws Exception {
    physicalInventoriesRepository.deleteAll();
    stockCardRepository.deleteAll();
    stockEventsRepository.deleteAll();
  }

  @Test
  public void should_not_save_events_if_anything_goes_wrong_in_validations_service()
      throws Exception {
    //given
    StockEventDto stockEventDto = createStockEventDto();
    stockEventDto.setUserId(userId);
    setContext(stockEventDto);

    Mockito.doThrow(new RuntimeException("something wrong from validations service"))
        .when(stockEventValidationsService)
        .validate(stockEventDto);

    //when
    try {
      stockEventProcessor.process(stockEventDto);
    } catch (RuntimeException ex) {
      //then
      assertSize(cardSize, eventSize, lineItemSize);
      return;
    }

    Assert.fail();
  }

  @Test
  public void shouldSaveEventAndLineItemsAndCallNotificationsWhenValidationServicePasses() 
      throws Exception {
    StockEventDto stockEventDto = createStockEventDto();
    stockEventDto.setUserId(userId);
    setContext(stockEventDto);

    //when
    stockEventProcessor.process(stockEventDto);

    //then
    assertSize(cardSize + 1, eventSize + 1, lineItemSize + 1);
    verify(stockEventNotificationProcessor).callAllNotifications(stockEventDto);
  }

  @Test
  public void shouldSubmitPhysicalInventoryWhenEventIsAboutPhysicalInventory()
      throws Exception {
    //given
    StockEventDto stockEventDto = createStockEventDto();
    stockEventDto.setUserId(userId);
    stockEventDto.getLineItems().get(0).setReasonId(null);
    stockEventDto.getLineItems().get(0).setSourceId(null);
    stockEventDto.getLineItems().get(0).setDestinationId(null);
    setContext(stockEventDto);

    //when
    stockEventProcessor.process(stockEventDto);

    //then
    verify(physicalInventoryService)
        .submitPhysicalInventory(any(PhysicalInventoryDto.class), any(UUID.class));
  }

  private void assertSize(long cardSize, long eventSize, long lineItemSize) {
    assertThat(stockCardRepository.count(), is(cardSize));
    assertThat(stockEventsRepository.count(), is(eventSize));
    assertThat(lineItemRepository.count(), is(lineItemSize));
  }
}
