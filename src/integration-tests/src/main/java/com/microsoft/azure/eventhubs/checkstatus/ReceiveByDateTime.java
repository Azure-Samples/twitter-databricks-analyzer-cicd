/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.eventhubs.checkstatus;

import com.microsoft.azure.eventhubs.ConnectionStringBuilder;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventHubException;
import com.microsoft.azure.eventhubs.EventPosition;
import com.microsoft.azure.eventhubs.EventHubRuntimeInformation;
import com.microsoft.azure.eventhubs.PartitionReceiver;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.*;

public class ReceiveByDateTime {

    public static void main(String[] args)
            throws EventHubException, ExecutionException, InterruptedException, IOException {

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime argStartTime = null;

        if (args.length == 0) {
            System.out.println("Please provide a start date as the first argument");
            System.exit(1);
        }

        try {
            argStartTime = LocalDateTime.parse(args[0], formatter);
            System.out.println("Searching events after: " + argStartTime);
        } catch (Exception e) {
            System.out.println("There was an exception: " + e.toString());
            System.exit(1);
        }

        final LocalDateTime startTime = argStartTime;

        String namespaceName = System.getenv("EVENTHUB_NAMESPACE");
        String eventHubName = System.getenv("EVENTHUB_ALERTS");
        String sasKeyName = System.getenv("EVENTHUB_KEY_NAME");
        String sasKey = System.getenv("EVENTHUB_KEY");

        if (namespaceName == null || namespaceName.isEmpty()) {
            System.out.println("Please make sure to set EVENTHUB_NAMESPACE");
            System.exit(1);
        }
        if (eventHubName == null || eventHubName.isEmpty()) {
            System.out.println("Please make sure to set EVENTHUB_ALERTS");
            System.exit(1);
        }
        if (sasKeyName == null || sasKeyName.isEmpty()) {
            System.out.println("Please make sure to set EVENTHUB_KEY_NAME");
            System.exit(1);
        }
        if (sasKey == null || sasKey.isEmpty()) {
            System.out.println("Please make sure to set EVENTHUB_KEY");
            System.exit(1);
        }

        final ConnectionStringBuilder connStr = new ConnectionStringBuilder().setNamespaceName(namespaceName)
                .setEventHubName(eventHubName).setSasKeyName(sasKeyName).setSasKey(sasKey);

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final EventHubClient ehClient = EventHubClient.createSync(connStr.toString(), executorService);

        final EventHubRuntimeInformation eventHubInfo = ehClient.getRuntimeInformation().get();
        final String[] partitionIds = eventHubInfo.getPartitionIds();

        try {
            String partitionId = partitionIds[0];

            final PartitionReceiver receiver = ehClient.createEpochReceiverSync(
                    EventHubClient.DEFAULT_CONSUMER_GROUP_NAME, partitionId,
                    EventPosition.fromEnqueuedTime(Instant.EPOCH), 2345);

            System.out.println("date-time receiver created...");

            try {
                final LocalDateTime checkupStartTime = LocalDateTime.now();

                // Making sure 15 minutes haven't passed since the test started
                while (LocalDateTime.now().minusMinutes(15).isBefore(checkupStartTime)) {
                    receiver.receive(100).thenAcceptAsync(receivedEvents -> {
                        int batchSize = 0;
                        if (receivedEvents != null) {
                            for (EventData receivedEvent : receivedEvents) {
                                System.out.print(String.format("[%s] Offset: %s, #: %s, Time: %s, PT: %s, ",
                                        new java.util.Date(), receivedEvent.getSystemProperties().getOffset(),
                                        receivedEvent.getSystemProperties().getSequenceNumber(),
                                        receivedEvent.getSystemProperties().getEnqueuedTime(), partitionId));

                                if (receivedEvent.getBytes() != null) {

                                    try {
                                        final String dataString = new String(receivedEvent.getBytes(), "UTF8");
                                        final JSONObject obj = new JSONObject(dataString);
                                        final String windowStartString = obj.getString("windowStart");
                                        final LocalDateTime eventDateTime = LocalDateTime.parse(windowStartString,
                                                formatter);
                                        System.out.println("Event Time: " + windowStartString);

                                        if (eventDateTime.isAfter(startTime)) {
                                            System.out.println("Found a processed alert: " + dataString);
                                            System.exit(0);
                                        }
                                    } catch (Exception e) {
                                        System.out.println("There was a problem parsing the date of the event: "
                                                + receivedEvent.getBytes());
                                        continue;
                                    }
                                }
                                batchSize++;
                            }
                        }

                        System.out.println(String.format("ReceivedBatch Size: %s", batchSize));
                    }, executorService).get();
                }

                System.out.println("Could not find a contemporary alert for 15 minutes.");
                System.exit(1);
            } finally {
                // cleaning up receivers is paramount;
                // Quota limitation on maximum number of concurrent receivers per consumergroup
                // per partition is 5
                receiver.close().thenComposeAsync(aVoid -> ehClient.close(), executorService)
                        .whenCompleteAsync((t, u) -> {
                            if (u != null) {
                                // wire-up this error to diagnostics infrastructure
                                System.out.println(String.format("closing failed with error: %s", u.toString()));
                            }
                        }, executorService).get();
            }
        } finally {
            executorService.shutdown();
        }
    }
}
