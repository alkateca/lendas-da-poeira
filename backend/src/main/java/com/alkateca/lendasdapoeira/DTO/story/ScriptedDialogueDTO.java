package com.alkateca.lendasdapoeira.DTO.story;

import java.util.List;

public record ScriptedDialogueDTO(
    String trigger,          // ON_HERO_DEATH, ON_CARD_PLAYED, ON_LOW_HP, ON_TURN_START
    String targetHeroName,
    String targetCardName,
    Integer turnNumber,
    String speaker,
    String dialogue
) {}
