/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.ExperienceEvent
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.integration.util.IntegrationTestConstants.EdgeLocationHint
import com.adobe.marketing.mobile.edge.integration.util.TestSetupHelper
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.TestableNetworkRequest
import com.adobe.marketing.mobile.util.AnyOrderMatch
import com.adobe.marketing.mobile.util.CollectionEqualCount
import com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch
import com.adobe.marketing.mobile.util.JSONAsserts.assertTypeMatch
import com.adobe.marketing.mobile.util.MonitorExtension
import com.adobe.marketing.mobile.util.NodeConfig.Scope.Subtree
import com.adobe.marketing.mobile.util.RealNetworkService
import com.adobe.marketing.mobile.util.TestConstants
import com.adobe.marketing.mobile.util.TestHelper
import com.adobe.marketing.mobile.util.ValueExactMatch
import com.adobe.marketing.mobile.util.ValueTypeMatch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

/**
 * Performs validation on integration with the Edge Network upstream service.
 */
@RunWith(AndroidJUnit4::class)
class UpstreamIntegrationTests {
    private val realNetworkService = RealNetworkService()
    private val edgeLocationHint: String? = TestSetupHelper.defaultLocationHint
    private val tagsMobilePropertyId: String = TestSetupHelper.defaultTagsMobilePropertyId

    @JvmField
    @Rule
    var rule: RuleChain = RuleChain.outerRule(TestHelper.LogOnErrorRule())
        .around(TestHelper.SetupCoreRule())

    @Before
    @Throws(Exception::class)
    fun setup() {
        println("Environment var - Edge Network tags mobile property ID: $tagsMobilePropertyId")
        println("Environment var - Edge Network location hint: $edgeLocationHint")
        ServiceProvider.getInstance().networkService = realNetworkService

        // Set the tags mobile property ID for a specific Edge Network environment
        MobileCore.configureWithAppID(tagsMobilePropertyId)

        // Use expectation to guarantee Configuration shared state availability
        // Required primarily by `createInteractURL`
        TestHelper.setExpectationEvent(
            TestConstants.EventType.CONFIGURATION,
            TestConstants.EventSource.RESPONSE_CONTENT,
            1
        )
        val latch = CountDownLatch(1)
        MobileCore.registerExtensions(
            listOf(
                Edge.EXTENSION,
                Identity.EXTENSION,
                MonitorExtension.EXTENSION
            )
        ) {
            latch.countDown()
        }
        latch.await()
        TestHelper.assertExpectedEvents(true)

        // Set Edge location hint if one is set for the test suite
        TestSetupHelper.setInitialLocationHint(edgeLocationHint)

        resetTestExpectations()
    }

    @After
    fun tearDown() {
        resetTestExpectations()
        // Clear any updated configuration
        MobileCore.clearUpdatedConfiguration()
    }

    /**
     * Tests that a standard sendEvent receives a single network response with HTTP code 200.
     */
    @Test
    fun testSendEvent_receivesExpectedNetworkResponse() {
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        Edge.sendEvent(experienceEvent) {}

        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(200, matchingResponses?.firstOrNull()?.responseCode)
    }

    /**
     * Tests that a standard sendEvent receives a single network response with HTTP code 200.
     */
    @Test
    fun testSendEvent_whenComplexEvent_receivesExpectedNetworkResponse() {
        // Setup
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)

        val xdm = mapOf(
            "testString" to "xdm"
        )

        val data = mapOf(
            "testDataString" to "stringValue",
            "testDataInt" to 101,
            "testDataBool" to true,
            "testDataDouble" to 13.66,
            "testDataArray" to listOf("arrayElem1", 2, true),
            "testDataDictionary" to mapOf("key" to "val")
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(xdm)
            .setData(data)
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Verify
        // Network response assertions
        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(200, matchingResponses?.firstOrNull()?.responseCode)
    }

    /**
     * Tests that a standard sendEvent() receives a single network response with HTTP code 200
     */
    @Test
    fun testSendEvent_whenComplexXDMEvent_receivesExpectedNetworkResponse() {
        // Setup
        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)

        val xdm: Map<String, Any> = mapOf(
            "testString" to "xdm",
            "testInt" to 10,
            "testBool" to false,
            "testDouble" to 12.89,
            "testArray" to listOf("arrayElem1", 2, true),
            "testDictionary" to mapOf("key" to "val")
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(xdm)
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Verify
        // Network response assertions
        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(200, matchingResponses?.firstOrNull()?.responseCode)
    }

    /**
     * Tests that a standard sendEvent receives the expected event handles.
     */
    @Test
    fun testSendEvent_receivesExpectedEventHandles() {
        // Setup
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT,
            expectedCount = 1
        )
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.STATE_STORE,
            expectedCount = 1
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Verify
        TestHelper.assertExpectedEvents(true)
    }

    /**
     * Tests that a standard sendEvent receives the expected event handles and does not receive an error event.
     */
    @Test
    fun testSendEvent_doesNotReceivesErrorEvent() {
        // Setup
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT,
            expectedCount = 1
        )
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.STATE_STORE,
            expectedCount = 1
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Verify
        TestHelper.assertExpectedEvents(true)

        val errorEvents =
            TestSetupHelper.getEdgeEventHandles(expectedHandleType = TestConstants.EventSource.ERROR_RESPONSE_CONTENT)
        assertEquals(0, errorEvents.size)
    }

    /**
     * Tests that a standard sendEvent with no prior location hint value set receives the expected location hint event handle.
     * That is, checks for a string type location hint.
     */
    @Test
    fun testSendEvent_with_NO_priorLocationHint_receivesExpectedLocationHintEventHandle() {
        // Setup
        // Clear any existing location hint
        Edge.setLocationHint(null)

        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT,
            expectedCount = 1
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Verify
        val expectedLocationHint = """
        {
          "payload": [
            {
              "ttlSeconds" : 123,
              "scope" : "EdgeNetwork",
              "hint" : "STRING_TYPE"
            }
          ]
        }
        """

        // Unsafe access used since testSendEvent_receivesExpectedEventHandles guarantees existence
        val locationHintResult = TestSetupHelper.getEdgeEventHandles(expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT)
            .first()

        assertTypeMatch(
            expectedLocationHint,
            locationHintResult.eventData,
            ValueExactMatch("payload[0].scope"),
            AnyOrderMatch("payload[0]")
        )
    }

    /**
     * Tests that a standard sendEvent WITH prior location hint value set receives the expected location hint event handle.
     * That is, checks for consistency between prior location hint value and received location hint result.
     */
    @Test
    fun testSendEvent_withPriorLocationHint_receivesExpectedLocationHintEventHandle() {
        // Uses all the valid location hint cases in random order to prevent order dependent edge cases slipping through
        EdgeLocationHint.values().map { it.rawValue }.shuffled().forEach { locationHint ->
            // Setup
            Edge.setLocationHint(locationHint)

            TestSetupHelper.expectEdgeEventHandle(
                expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT,
                expectedCount = 1
            )

            val experienceEvent = ExperienceEvent.Builder()
                .setXdmSchema(mapOf("xdmtest" to "data"))
                .setData(mapOf("data" to mapOf("test" to "data")))
                .build()

            // Test
            Edge.sendEvent(experienceEvent) {}

            // Verify
            val expectedLocationHint = """
            {
              "payload": [
                {
                  "ttlSeconds" : 123,
                  "scope" : "EdgeNetwork",
                  "hint" : "$locationHint"
                }
              ]
            }
            """

            // Unsafe access used since testSendEvent_receivesExpectedEventHandles guarantees existence
            val locationHintResult = TestSetupHelper.getEdgeEventHandles(expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT)
                .first()
            assertExactMatch(
                expectedLocationHint,
                locationHintResult.eventData,
                ValueTypeMatch("payload[*].ttlSeconds"),
                AnyOrderMatch("payload[0]")
            )

            resetTestExpectations()
        }
    }

    /**
     * Tests that a standard sendEvent with no prior state receives the expected state store event handle.
     */
    @Test
    fun testSendEvent_with_NO_priorState_receivesExpectedStateStoreEventHandle() {
        // Setup
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.STATE_STORE,
            expectedCount = 1
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Verify
        val expectedStateStore = """
        {
          "payload": [
            {
              "maxAge": 123,
              "key": "kndctr_972C898555E9F7BC7F000101_AdobeOrg_cluster",
              "value": "STRING_TYPE"
            },
            {
              "maxAge": 123,
              "key": "kndctr_972C898555E9F7BC7F000101_AdobeOrg_identity",
              "value": "STRING_TYPE"
            }
          ]
        }
        """

        // Unsafe access used since testSendEvent_receivesExpectedEventHandles guarantees existence
        val stateStoreEvent = TestSetupHelper.getEdgeEventHandles(expectedHandleType = TestConstants.EventSource.STATE_STORE)
            .last()

        assertTypeMatch(
            expectedStateStore,
            stateStoreEvent.eventData,
            ValueExactMatch("payload[*].key"),
            // Used to strictly validate `payload` and elements under it have same count as expected
            CollectionEqualCount(Subtree, "payload") 
        )
    }

    /**
     * Tests that a standard sendEvent with prior state receives the expected state store event handle.
     */
    @Test
    fun testSendEvent_withPriorState_receivesExpectedStateStoreEventHandle() {
        // Setup
        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        TestSetupHelper.expectEdgeEventHandle(TestConstants.EventSource.STATE_STORE)

        Edge.sendEvent(experienceEvent) {}

        // Assert on expected state store event to properly wait for the initial state setup to complete
        // before moving on to test case logic
        TestHelper.assertExpectedEvents(true)

        resetTestExpectations()

        EdgeLocationHint.values().map { it.rawValue }.shuffled().forEach { locationHint ->
            // Set location hint
            Edge.setLocationHint(locationHint)

            TestSetupHelper.expectEdgeEventHandle(TestConstants.EventSource.STATE_STORE)

            // Test
            Edge.sendEvent(experienceEvent) {}

            // Verify
            val expectedStateStore = """
            {
              "payload": [
                {
                  "maxAge": 123,
                  "key": "kndctr_972C898555E9F7BC7F000101_AdobeOrg_cluster",
                  "value": "$locationHint"
                }
              ]
            }
            """

            // Unsafe access used since testSendEvent_receivesExpectedEventHandles guarantees existence
            val stateStoreEvent = TestSetupHelper.getEdgeEventHandles(TestConstants.EventSource.STATE_STORE)
                .last()

            assertExactMatch(
                expectedStateStore,
                stateStoreEvent.eventData,
                ValueTypeMatch("payload[0].maxAge")
            )

            resetTestExpectations()
        }
    }

    // 2nd event tests
    /**
     * Tests that sending two standard sendEvents receives the expected network response.
     */
    @Test
    fun testSendEventx2_receivesExpectedNetworkResponse() {
        // Setup
        // These expectations are used as a barrier for the event processing to complete
        TestSetupHelper.expectEdgeEventHandle(expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT)

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Extract location hint from Edge Network location hint response event
        val locationHintResult = TestSetupHelper.getLastLocationHintResultValue()

        if (locationHintResult.isNullOrEmpty()) {
            fail("Unable to extract valid location hint from location hint result event handle.")
        }

        // If there is a location hint preset for the test suite, check consistency between it and the
        // value from the Edge Network
        if (!edgeLocationHint.isNullOrEmpty()) {
            assertEquals(edgeLocationHint, locationHintResult)
        }

        // Wait on all expectations to finish processing before clearing expectations
        TestHelper.assertExpectedEvents(true)

        // Reset all test expectations
        resetTestExpectations()

        // Set actual testing expectations
        // If test suite level location hint is not set, uses the value extracted from location hint result
        val locationHintNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHintResult),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(locationHintNetworkRequest, expectedCount = 1)

        // Test
        // 2nd event
        Edge.sendEvent(experienceEvent) {}

        // Verify
        // Network response assertions
        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(locationHintNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(200, matchingResponses?.firstOrNull()?.responseCode)
    }

    /**
     * Tests that sending two standard sendEvents receives the expected event handles.
     */
    @Test
    fun testSendEventx2_receivesExpectedEventHandles() {
        // Setup
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT,
            expectedCount = 2
        )
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.STATE_STORE,
            expectedCount = 2
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}
        Edge.sendEvent(experienceEvent) {}

        // Verify
        TestHelper.assertExpectedEvents(true)
    }

    /**
     * Tests that sending two standard sendEvents does not receive any error event.
     */
    @Test
    fun testSendEventx2_doesNotReceivesErrorEvent() {
        // Setup
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT,
            expectedCount = 2
        )
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.STATE_STORE,
            expectedCount = 2
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}
        Edge.sendEvent(experienceEvent) {}

        // Verify
        TestHelper.assertExpectedEvents(true)

        val errorEvents =
            TestSetupHelper.getEdgeEventHandles(expectedHandleType = TestConstants.EventSource.ERROR_RESPONSE_CONTENT)
        assertEquals(0, errorEvents.size)
    }

    /**
     * Tests that sending two standard sendEvents receives the expected location hint event handle.
     * It verifies the consistency of location hint between the first and second event handles.
     */
    @Test
    fun testSendEventx2_receivesExpectedLocationHintEventHandle() {
        // Setup
        TestSetupHelper.expectEdgeEventHandle(expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT)

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        Edge.sendEvent(experienceEvent) {}

        // Extract location hint from Edge Network location hint response event
        val locationHintResult = TestSetupHelper.getLastLocationHintResultValue()
        if (locationHintResult.isNullOrEmpty()) {
            fail("Unable to extract valid location hint from location hint result event handle.")
        }

        // Reset all test expectations
        resetTestExpectations()

        // Set actual testing expectations
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT,
            expectedCount = 1
        )

        // Test
        // 2nd event
        Edge.sendEvent(experienceEvent) {}

        // Verify
        // If there is a location hint preset for the test suite, check consistency between it and the
        // value from the Edge Network
        if (!edgeLocationHint.isNullOrEmpty()) {
            assertEquals(edgeLocationHint, locationHintResult)
        }

        // Verify location hint consistency between 1st and 2nd event handles
        val expectedLocationHint = """
        {
          "payload": [
            {
              "ttlSeconds" : 123,
              "scope" : "EdgeNetwork",
              "hint" : "$locationHintResult"
            }
          ]
        }
        """

        // Unsafe access used since testSendEvent_receivesExpectedEventHandles guarantees existence
        val locationHintResultEvent = TestSetupHelper.getEdgeEventHandles(expectedHandleType = TestConstants.EventSource.LOCATION_HINT_RESULT)
            .first()
        assertExactMatch(
            expectedLocationHint,
            locationHintResultEvent.eventData,
            ValueTypeMatch("payload[0].ttlSeconds"),
            AnyOrderMatch("payload[0]")
        )
    }

    /**
     * Tests that an invalid datastream ID returns the expected error.
     */
    @Test
    fun testSendEvent_withInvalidDatastreamID_receivesExpectedError() {
        // Setup
        TestHelper.setExpectationEvent(
            TestConstants.EventType.CONFIGURATION,
            TestConstants.EventSource.RESPONSE_CONTENT,
            1
        )

        MobileCore.updateConfiguration(mapOf("edge.configId" to "12345-example"))

        TestHelper.assertExpectedEvents(true)

        val interactNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = edgeLocationHint),
            HttpMethod.POST
        )
        realNetworkService.setExpectationForNetworkRequest(interactNetworkRequest, expectedCount = 1)
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.ERROR_RESPONSE_CONTENT,
            expectedCount = 1
        )

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Verify
        // Network response assertions
        realNetworkService.assertAllNetworkRequestExpectations()
        val matchingResponses = realNetworkService.getResponsesFor(interactNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        assertEquals(400, matchingResponses?.firstOrNull()?.responseCode)

        // Event assertions
        val expectedError = """
        {
            "status": 400,
            "detail": "STRING_TYPE",
            "report": {
                "requestId": "STRING_TYPE"
            },
            "requestEventId": "STRING_TYPE",
            "title": "Invalid datastream ID",
            "type": "https://ns.adobe.com/aep/errors/EXEG-0003-400",
            "requestId": "STRING_TYPE"
        }
        """

        val errorEvents = TestSetupHelper.getEdgeResponseErrors()

        assertEquals(1, errorEvents.size)

        val errorEvent = errorEvents.first()
        assertTypeMatch(
            expectedError,
            errorEvent.eventData,
            ValueExactMatch("status", "title", "type")
        )
    }

    /**
     * Validates that an invalid location hint returns the expected error with 0 byte data body.
     */
    @Test
    fun testSendEvent_withInvalidLocationHint_receivesExpectedError() {
        // Setup
        val invalidNetworkRequest = TestableNetworkRequest(
            TestSetupHelper.createInteractURL(locationHint = "invalid"),
            HttpMethod.POST
        )

        realNetworkService.setExpectationForNetworkRequest(networkRequest = invalidNetworkRequest, expectedCount = 1)
        TestSetupHelper.expectEdgeEventHandle(
            expectedHandleType = TestConstants.EventSource.ERROR_RESPONSE_CONTENT,
            expectedCount = 1
        )

        Edge.setLocationHint("invalid")

        val experienceEvent = ExperienceEvent.Builder()
            .setXdmSchema(mapOf("xdmtest" to "data"))
            .setData(mapOf("data" to mapOf("test" to "data")))
            .build()

        // Test
        Edge.sendEvent(experienceEvent) {}

        // Verify
        realNetworkService.assertAllNetworkRequestExpectations()

        val matchingResponses = realNetworkService.getResponsesFor(invalidNetworkRequest)

        assertEquals(1, matchingResponses?.size)
        // Convenience to assert directly on the first element in the rest of the test case
        val matchingResponse = matchingResponses?.firstOrNull()
        assertEquals(404, matchingResponse?.responseCode)

        val contentLengthHeader = matchingResponse?.getResponsePropertyValue("Content-Length")
        val contentLength = contentLengthHeader?.toIntOrNull()

        if (contentLength != null) {
            println("Content-Length: $contentLength")
            assertEquals(0, contentLength)
        } else {
            println("Content-Length header not found or not a valid integer")
        }

        // Should be null when there is no response body from the server
        val responseBodySize = matchingResponse?.inputStream?.readBytes()?.size
        assertNull(responseBodySize)

        // Error event assertions
        TestHelper.assertExpectedEvents(true)
    }

    /**
     * Resets all test helper expectations and recorded data
     */
    private fun resetTestExpectations() {
        realNetworkService.reset()
        TestHelper.resetTestExpectations()
    }
}
