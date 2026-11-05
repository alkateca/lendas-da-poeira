package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.story.ScriptedDialogueDTO;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class StoryTriggerEvaluator {

    public List<ScriptedDialogueDTO> evaluateCardPlayed(
            StoryMissionDefinition mission,
            Set<String> alreadyTriggeredKeys,
            String playedCardName
    ) {
        if (mission == null || mission.scriptedEvents() == null || playedCardName == null) {
            return List.of();
        }
        List<ScriptedDialogueDTO> triggered = new ArrayList<>();
        for (ScriptedDialogueDTO ev : mission.scriptedEvents()) {
            if ("ON_CARD_PLAYED".equalsIgnoreCase(ev.trigger())
                    && playedCardName.equalsIgnoreCase(ev.targetCardName())) {
                String key = "CARD:" + playedCardName.toLowerCase();
                if (alreadyTriggeredKeys.add(key)) {
                    triggered.add(ev);
                }
            }
        }
        return triggered;
    }

    public List<ScriptedDialogueDTO> evaluateHeroDeath(
            StoryMissionDefinition mission,
            Set<String> alreadyTriggeredKeys,
            String deadHeroName
    ) {
        if (mission == null || mission.scriptedEvents() == null || deadHeroName == null) {
            return List.of();
        }
        List<ScriptedDialogueDTO> triggered = new ArrayList<>();
        for (ScriptedDialogueDTO ev : mission.scriptedEvents()) {
            if ("ON_HERO_DEATH".equalsIgnoreCase(ev.trigger())
                    && deadHeroName.equalsIgnoreCase(ev.targetHeroName())) {
                String key = "DEATH:" + deadHeroName.toLowerCase();
                if (alreadyTriggeredKeys.add(key)) {
                    triggered.add(ev);
                }
            }
        }
        return triggered;
    }
}
