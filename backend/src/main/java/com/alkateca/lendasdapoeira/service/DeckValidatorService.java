package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.Colors;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeckValidatorService {

    public static class ValidationResult {
        public boolean isValidForPlay;
        public String message;

        public ValidationResult(boolean isValidForPlay, String message) {
            this.isValidForPlay = isValidForPlay;
            this.message = message;
        }
    }

    public ValidationResult validateDeck(Deck deck) {
        List<HeroCard> heroes = deck.getHeroList();
        List<Card> cards = deck.getCardList();

        // 1. Regra de Heróis (Exatamente 3)
        if (heroes == null || heroes.size() != 3) {
            return new ValidationResult(false, "O baralho deve conter exatamente 3 Heróis.");
        }

        // 2. Regra da Cor Obrigatória (Mesma cor ou Coringa White)
        Colors baseColor = null;
        for (HeroCard h : heroes) {
            // Pega a cor do enum, caindo para NEUTRAL caso seja nulo
            Colors color = h.getColor() != null ? h.getColor() : Colors.NEUTRAL;

            // Pula se for o coringa WHITE
            if (color == Colors.WHITE) continue;

            if (baseColor == null) {
                baseColor = color;
            } else if (baseColor != color) {
                return new ValidationResult(false, "Todos os heróis não coringas devem ser da mesma cor!");
            }
        }

        // 3. Regra de Contagem Total de Cartas (Exatamente 30)
        int totalCards = cards != null ? cards.size() : 0;
        if (totalCards != 30) {
            return new ValidationResult(false, "O baralho salvo possui " + totalCards + "/30 cartas. (Incompleto para jogos)");
        }

        // 4. Regra de Limite de Cópias (Máx 3 normais, Máx 1 se unica=true)
        Map<String, Integer> cardCounts = new HashMap<>();
        Map<String, Boolean> cardIsUnique = new HashMap<>();

        for (Card c : cards) {
            String name = c.getCardName();
            cardCounts.put(name, cardCounts.getOrDefault(name, 0) + 1);
            cardIsUnique.put(name, c.isUnica());
        }

        for (Map.Entry<String, Integer> entry : cardCounts.entrySet()) {
            String name = entry.getKey();
            int count = entry.getValue();
            boolean isUnique = cardIsUnique.getOrDefault(name, false);

            if (isUnique && count > 1) {
                return new ValidationResult(false, "A carta única '" + name + "' excede o limite de 1 cópia.");
            }
            if (!isUnique && count > 3) {
                return new ValidationResult(false, "A carta '" + name + "' excede o limite de 3 cópias.");
            }
        }

        return new ValidationResult(true, "Baralho válido e pronto para combate!");
    }
}