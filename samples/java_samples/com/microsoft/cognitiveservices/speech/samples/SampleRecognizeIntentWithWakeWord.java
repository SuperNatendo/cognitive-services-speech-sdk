package com.microsoft.cognitiveservices.speech.samples;
//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel;
import com.microsoft.cognitiveservices.speech.SessionEventType;
import com.microsoft.cognitiveservices.speech.SpeechFactory;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer;
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel;

public class SampleRecognizeIntentWithWakeWord implements Runnable, Stoppable {
    private static final String delimiter = "\n";
    private final List<String> content = new ArrayList<>();
    private boolean continuousListeningStarted = false;
    private IntentRecognizer reco = null;
    private String buttonText = "";

    @Override
    public  void stop()
    {
        if (continuousListeningStarted) {
            if (reco != null) {
                Future<?> task = reco.stopKeywordRecognitionAsync();
                SampleSettings.setOnTaskCompletedListener(task, result -> {
                    reco.close();

                    System.out.println("Continuous recognition stopped.");
                    System.out.println(buttonText);
                    continuousListeningStarted = false;
                });
            } else {
                continuousListeningStarted = false;
            }

            return;
        }

    }
 
    ///////////////////////////////////////////////////
    // recognize intent with wake word
    ///////////////////////////////////////////////////
    @Override
    public void run () {
        if (continuousListeningStarted) {
            return;
        }
        
        HashMap<String, String> intentIdMap = new HashMap<>();
        intentIdMap.put("1", "play music");
        intentIdMap.put("2", "stop");

        // create factory
        SpeechFactory factory = SampleSettings.getFactory();

        content.clear();
        content.add("");
        content.add("");
        try {
            // Note: to use the microphone, replace the parameter with "new MicrophoneAudioInputStream()"
            reco = factory.createIntentRecognizer(SampleSettings.WaveFile);
            
            LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromSubscription(SampleSettings.LuisRegion,
                    SampleSettings.LuisSubscriptionKey, SampleSettings.LuisAppId);
            for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
                reco.addIntent(entry.getKey(), intentModel, entry.getValue());
            }

            reco.SessionEvent.addEventListener((o, sessionEventArgs) -> {
                System.out.println("got a session (" + sessionEventArgs.getSessionId() + ")event: "
                        + sessionEventArgs.getEventType());
                if (sessionEventArgs.getEventType() == SessionEventType.SessionStartedEvent) {
                    content.set(0, "KeywordModel `" + SampleSettings.Keyword + "` detected");
                    System.out.println(String.join(delimiter, content));
                    content.add("");
                }
            });

            reco.IntermediateResultReceived.addEventListener((o, intermediateResultEventArgs) -> {
                String s = intermediateResultEventArgs.getResult().getRecognizedText();
                System.out.println("got an intermediate result: " + s);
                Integer index = content.size() - 2;
                content.set(index + 1, index.toString() + ". " + s);
                System.out.println(String.join(delimiter, content));
            });

            reco.FinalResultReceived.addEventListener((o, finalResultEventArgs) -> {
                String s = finalResultEventArgs.getResult().getRecognizedText();
                String intentId = finalResultEventArgs.getResult().getIntentId();
                String intent = "";
                if (intentIdMap.containsKey(intentId)) {
                    intent = intentIdMap.get(intentId);
                }

                System.out.println("got a result: " + s);
                if (!s.isEmpty()) {
                    Integer index = content.size() - 2;
                    content.set(index + 1, index.toString() + ". " + s + " [intent: " + intent + "]");
                    content.set(0, "say `" + SampleSettings.Keyword + "`...");
                    System.out.println(String.join(delimiter, content));
                }
            });

            Future<?> task = reco.startKeywordRecognitionAsync(KeywordRecognitionModel.fromFile(SampleSettings.KeywordModel));
            SampleSettings.setOnTaskCompletedListener(task, result -> {
                content.set(0, "say `" + SampleSettings.Keyword + "`...");
                System.out.println(String.join(delimiter, content));
            });

            continuousListeningStarted = true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            SampleSettings.displayException(ex);
        }
    }
}