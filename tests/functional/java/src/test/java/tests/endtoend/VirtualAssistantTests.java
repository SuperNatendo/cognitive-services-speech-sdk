//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//

package tests.endtoend;

import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.dialog.BotConnectorActivity;
import com.microsoft.cognitiveservices.speech.dialog.BotConnectorConfig;
import com.microsoft.cognitiveservices.speech.dialog.SpeechBotConnector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tests.Settings;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.*;
import static tests.Settings.*;

public class VirtualAssistantTests {

    public static final String COMMUNICATION_TYPE_STRING = "Conversation_Communication_Type";
    public static final String AUTO_REPLY_CONNECTION_TYPE = "AutoReply";
    public static final String EN_US = "en-US";
    private SpeechBotConnector botConnector;
    private EventRecord eventRecord;

    private static String readFileAsString(String path) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
        String result = "";
        String line;

        while ((line = br.readLine()) != null)
        {
            result += line;
        }
        br.close();
        return result;
    }

    @BeforeClass
    public static void setupBeforeClass() {
        Settings.LoadSettings();
    }

    @Before
    public void setUp() {
        // Bot secret is not required when using AutoReply connection type
        final BotConnectorConfig botConnectorConfig = BotConnectorConfig.fromSecretKey(SpeechChannelSecretForVirtualAssistant, SpeechSubscriptionKeyForVirtualAssistant, SpeechRegionForVirtualAssistant);
        botConnectorConfig.setProperty(COMMUNICATION_TYPE_STRING, AUTO_REPLY_CONNECTION_TYPE);
        botConnectorConfig.setSpeechRecognitionLanguage(EN_US);

        // For tests we are using the wav file. For manual testing use fromDefaultMicrophone.
        final AudioConfig audioConfig = AudioConfig.fromWavFileInput(WavFile);

        botConnector = new SpeechBotConnector(botConnectorConfig, audioConfig);
        registerEventHandlers(botConnector);
        eventRecord = new EventRecord();
    }

    @Test
    public void testSendActivity() throws IOException {
        assertNotNull(botConnector);
        try {
            final String activity = readFileAsString(SerializedSpeechActivityFile);
            // Connect to the bot
            botConnector.connectAsync();
            botConnector.sendActivityAsync(BotConnectorActivity.fromSerializedActivity(activity));

            // Add a sleep for responses to arrive.
            for (int n = 1; n <= 20; n++) {
                try {
                    assertFalse(eventRecord.cancelledEventReceived);
                    assertTrue(eventRecord.activityEventReceived);
                   // assertTrue(eventRecord.eventWithAudioReceived); // Commenting until we fix bug id : 1780943
                } catch (AssertionError e) {
                    if (n == 20) {
                        throw e;
                    }
                    try {
                        Thread.sleep(1000 * n);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        } finally {
            // disconnect bot.
            botConnector.disconnectAsync();
        }
    }

    @Test
    public void testListenOnce() {
        try {
            // Connect to the bot
            botConnector.connectAsync();
            // Start listening.
            botConnector.listenOnceAsync();
            // Add a sleep for responses to arrive.
            for (int n = 1; n <= 20; n++) {
                try {
                    assertFalse(eventRecord.cancelledEventReceived);
                    assertTrue(eventRecord.recognizedEventReceived);
                    assertTrue(eventRecord.sessionStartedEventReceived);
                    assertTrue(eventRecord.sessionStoppedEventReceived);
                    assertTrue(eventRecord.activityEventReceived);
                } catch (AssertionError e) {
                    if (n == 20) {
                        throw e;
                    }
                    try {
                        Thread.sleep(1000 * n);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

        } finally {
            // disconnect bot.
            botConnector.disconnectAsync();
        }
    }

    private void registerEventHandlers(final SpeechBotConnector botConnector) {

        botConnector.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
            eventRecord.recognizedEventReceived = true;
        });

        botConnector.sessionStarted.addEventListener((o, sessionEventArgs) -> {
            eventRecord.sessionStartedEventReceived = true;
        });

        botConnector.sessionStopped.addEventListener((o, sessionEventArgs) -> {
            eventRecord.sessionStoppedEventReceived = true;
        });

        botConnector.canceled.addEventListener((o, canceledEventArgs) -> {
            eventRecord.cancelledEventReceived = true;
        });

        botConnector.activityReceived.addEventListener((o, activityEventArgs) -> {
            eventRecord.activityEventReceived = true;
            eventRecord.eventWithAudioReceived = activityEventArgs.hasAudio();
        });
    }

    private static class EventRecord {
        boolean sessionStartedEventReceived;
        boolean sessionStoppedEventReceived;
        boolean recognizedEventReceived;
        boolean cancelledEventReceived;
        boolean activityEventReceived;
        boolean eventWithAudioReceived;
    }
}