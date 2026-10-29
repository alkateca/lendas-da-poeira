package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

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

    public static class CatalogFilterCriteria {
        private String searchQuery;
        private Colors color;
        private CardType cardType;
        private ItemType itemType;
        private Raca raca;
        private Afinidade afinidade;
        private Classe classe;
        private Ordem ordem;
        private Integer empunhadura;
        private boolean onlyCompatibleWithHeroes = true;

        public String getSearchQuery() { return searchQuery; }
        public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
        public Colors getColor() { return color; }
        public void setColor(Colors color) { this.color = color; }
        public CardType getCardType() { return cardType; }
        public void setCardType(CardType cardType) { this.cardType = cardType; }
        public ItemType getItemType() { return itemType; }
        public void setItemType(ItemType itemType) { this.itemType = itemType; }
        public Raca getRaca() { return raca; }
        public void setRaca(Raca raca) { this.raca = raca; }
        public Afinidade getAfinidade() { return afinidade; }
        public void setAfinidade(Afinidade afinidade) { this.afinidade = afinidade; }
        public Classe getClasse() { return classe; }
        public void setClasse(Classe classe) { this.classe = classe; }
        public Ordem getOrdem() { return ordem; }
        public void setOrdem(Ordem ordem) { this.ordem = ordem; }
        public Integer getEmpunhadura() { return empunhadura; }
        public void setEmpunhadura(Integer empunhadura) { this.empunhadura = empunhadura; }
        public boolean isOnlyCompatibleWithHeroes() { return onlyCompatibleWithHeroes; }
        public void setOnlyCompatibleWithHeroes(boolean onlyCompatibleWithHeroes) { this.onlyCompatibleWithHeroes = onlyCompatibleWithHeroes; }
    }

    public ValidationResult validateDeck(Deck deck) {
        List<HeroCard> heroes = deck.getHeroList();
        List<Card> cards = deck.getCardList();
        List<Card> liberationList = deck.getLiberationList();

        // 1. Regra de Heróis (Exatamente 3)
        if (heroes == null || heroes.size() != 3) {
            return new ValidationResult(false, "O baralho deve conter exatamente 3 Heróis.");
        }

        // 2. Regra da Cor Obrigatória (Mesma cor ou Coringa White)
        Colors baseColor = null;
        for (HeroCard h : heroes) {
            Colors color = h.getColor() != null ? h.getColor() : Colors.NEUTRAL;
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

        // 4. Regra de Limite de Cópias (Máx 3 normais, Máx 1 se unica=true) e Compatibilidade com Equipe
        Map<String, Integer> cardCounts = new HashMap<>();
        Map<String, Boolean> cardIsUnique = new HashMap<>();

        for (Card c : cards) {
            if (!isCardCompatibleWithHeroes(c, heroes)) {
                return new ValidationResult(false, "A carta '" + c.getCardName() + "' é incompatível com os Heróis selecionados.");
            }
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

        // 5. Regra opcional da Carta de Liberação (Máx 1 e compatível com o Herói base se exigido)
        if (liberationList != null && !liberationList.isEmpty()) {
            if (liberationList.size() > 1) {
                return new ValidationResult(false, "O baralho pode conter no máximo 1 Carta de Liberação.");
            }
            Card libCard = liberationList.get(0);
            if (!isLiberationCardCompatibleWithHeroes(libCard, heroes)) {
                return new ValidationResult(false, "A Carta de Liberação '" + libCard.getCardName() + "' requer o Herói correspondente na equipe.");
            }
        }

        return new ValidationResult(true, "Baralho válido e pronto para combate!");
    }

    /**
     * Verifica se um Herói candidato é compatível com a equipe parcial (0 a 2 heróis já escolhidos).
     */
    public boolean isHeroCompatibleWithTeam(HeroCard candidate, List<HeroCard> selectedHeroes) {
        if (candidate == null) return false;
        if (selectedHeroes == null || selectedHeroes.isEmpty()) return true;
        if (selectedHeroes.size() >= 3) return false;

        boolean alreadyInTeam = selectedHeroes.stream()
                .anyMatch(h -> h.getCardName() != null && h.getCardName().equalsIgnoreCase(candidate.getCardName()));
        if (alreadyInTeam) return false;

        Colors candidateColor = candidate.getColor() != null ? candidate.getColor() : Colors.NEUTRAL;
        if (candidateColor == Colors.WHITE) return true;

        Colors existingNonWhite = selectedHeroes.stream()
                .map(h -> h.getColor() != null ? h.getColor() : Colors.NEUTRAL)
                .filter(c -> c != Colors.WHITE)
                .findFirst()
                .orElse(null);

        return existingNonWhite == null || existingNonWhite == candidateColor;
    }

    /**
     * Avalia dinamicamente se uma carta é compatível com os Heróis atualmente selecionados (1 a 3 heróis).
     */
    public boolean isCardCompatibleWithHeroes(Card card, List<HeroCard> selectedHeroes) {
        if (card == null) return false;
        if (selectedHeroes == null || selectedHeroes.isEmpty()) return true;

        boolean hasWhiteHero = selectedHeroes.stream().anyMatch(h -> h.getColor() == Colors.WHITE);
        Colors nonWhiteColor = selectedHeroes.stream()
                .map(HeroCard::getColor)
                .filter(c -> c != null && c != Colors.WHITE)
                .findFirst()
                .orElse(null);
        Colors baseColor = nonWhiteColor != null ? nonWhiteColor : (hasWhiteHero ? Colors.WHITE : null);

        Set<Afinidade> teamAffinities = new HashSet<>();
        Set<String> teamOrdersAndRaces = new HashSet<>();
        Set<Classe> teamClasses = new HashSet<>();
        boolean hasEmissor = false;
        boolean hasGoblin = false;

        for (HeroCard h : selectedHeroes) {
            if (h.getAfinidades() != null) teamAffinities.addAll(h.getAfinidades());
            if (h.getOrdem() != null) {
                for (Ordem o : h.getOrdem()) {
                    teamOrdersAndRaces.add(normalize(o.name()));
                    if (o.name().contains("GUARDI")) teamOrdersAndRaces.add("GUARDIAO DO TRONO");
                }
            }
            if (h.getRacas() != null) {
                for (Raca r : h.getRacas()) {
                    teamOrdersAndRaces.add(normalize(r.name()));
                    if (r.name().contains("GUARDI")) teamOrdersAndRaces.add("GUARDIAO DO TRONO");
                    if (r == Raca.GOBLIN) hasGoblin = true;
                }
            }
            if (h.getClasse() != null) {
                teamClasses.addAll(h.getClasse());
                if (h.getClasse().contains(Classe.EMISSOR)) hasEmissor = true;
            }
        }

        Colors cardColor = card.getColor() != null ? card.getColor() : Colors.NEUTRAL;

        // 1. Validação de Cor (Regra do Coringa WHITE ou Mesma Cor dos Heróis)
        if (!hasGoblin) {
            if (hasWhiteHero) {
                if (cardColor != Colors.WHITE && cardColor != Colors.NEUTRAL) return false;
            } else if (baseColor != null) {
                if (cardColor != baseColor && cardColor != Colors.NEUTRAL) return false;
            }
        }

        // 2. Validação de Afinidade Elemental e Magias (SPELL)
        List<Afinidade> reqAffinities = card.getAfinidades() != null ? card.getAfinidades() : List.of();
        if (!reqAffinities.isEmpty()) {
            boolean matchAff = reqAffinities.stream().anyMatch(teamAffinities::contains);
            if (!matchAff && !hasEmissor && !hasGoblin) return false;
        } else if (card.getCardType() == CardType.SPELL && teamAffinities.isEmpty() && !hasEmissor && !hasGoblin) {
            return false;
        }

        // 3. Validação de Ordem / Raça
        List<String> reqOrdersAndRaces = new ArrayList<>();
        if (card.getOrdem() != null) {
            card.getOrdem().forEach(o -> reqOrdersAndRaces.add(normalize(o.name())));
        }
        if (card.getRacas() != null) {
            card.getRacas().forEach(r -> reqOrdersAndRaces.add(normalize(r.name())));
        }
        if (!reqOrdersAndRaces.isEmpty() && !hasGoblin) {
            boolean matchOrderOrRace = reqOrdersAndRaces.stream().anyMatch(req ->
                    teamOrdersAndRaces.contains(req)
                            || (req.contains("GUARDI") && teamOrdersAndRaces.contains("GUARDIAO DO TRONO"))
            );
            if (!matchOrderOrRace) return false;
        }

        // 4. Validação de Classe
        List<Classe> reqClasses = card.getClasse() != null ? card.getClasse() : List.of();
        if (!reqClasses.isEmpty() && !hasGoblin) {
            boolean matchClass = reqClasses.stream().anyMatch(teamClasses::contains);
            if (!matchClass) return false;
        }

        return true;
    }

    public boolean isLiberationCardCompatibleWithHeroes(Card libCard, List<HeroCard> selectedHeroes) {
        if (libCard == null) return false;
        if (selectedHeroes == null || selectedHeroes.isEmpty()) return true;

        if (libCard.getTargetBaseHeroName() != null && !libCard.getTargetBaseHeroName().isBlank()) {
            return selectedHeroes.stream()
                    .anyMatch(h -> h.getCardName() != null
                            && h.getCardName().equalsIgnoreCase(libCard.getTargetBaseHeroName()));
        }
        return isCardCompatibleWithHeroes(libCard, selectedHeroes);
    }

    /**
     * Filtra uma lista de cartas aplicando todos os seletores de atributos, busca textual
     * e compatibilidade dinâmica com os Heróis atualmente selecionados (1 a 3 heróis).
     */
    public List<Card> filterCatalogCards(
            List<Card> catalogCards,
            List<HeroCard> selectedHeroes,
            CatalogFilterCriteria criteria) {

        if (catalogCards == null) return List.of();
        if (criteria == null) criteria = new CatalogFilterCriteria();

        final CatalogFilterCriteria f = criteria;
        final String query = f.getSearchQuery() != null ? normalize(f.getSearchQuery()) : "";

        return catalogCards.stream()
                .filter(card -> {
                    if (card == null) return false;

                    // 1. Filtragem dinâmica por Heróis selecionados
                    if (f.isOnlyCompatibleWithHeroes() && selectedHeroes != null && !selectedHeroes.isEmpty()) {
                        if (card.isLiberationCard()) {
                            if (!isLiberationCardCompatibleWithHeroes(card, selectedHeroes)) return false;
                        } else {
                            if (!isCardCompatibleWithHeroes(card, selectedHeroes)) return false;
                        }
                    }

                    // 2. Busca Textual (Nome ou Descrição)
                    if (!query.isEmpty()) {
                        String nameNorm = normalize(card.getCardName());
                        String descNorm = normalize(card.getDescricao());
                        if (!nameNorm.contains(query) && !descNorm.contains(query)) return false;
                    }

                    // 3. Cor / Elemento
                    if (f.getColor() != null) {
                        Colors cColor = card.getColor() != null ? card.getColor() : Colors.NEUTRAL;
                        if (cColor != f.getColor()) return false;
                    }

                    // 4. Tipo de Carta
                    if (f.getCardType() != null && card.getCardType() != f.getCardType()) {
                        return false;
                    }

                    // 5. Subtipo de Equipamento
                    if (f.getItemType() != null) {
                        ItemType iType = card.getItemType() != null ? card.getItemType() : ItemType.NENHUM;
                        if (iType != f.getItemType()) return false;
                    }

                    // 6. Raça
                    if (f.getRaca() != null) {
                        if (card.getRacas() == null || !card.getRacas().contains(f.getRaca())) return false;
                    }

                    // 7. Afinidade
                    if (f.getAfinidade() != null) {
                        if (card.getAfinidades() == null || !card.getAfinidades().contains(f.getAfinidade())) return false;
                    }

                    // 8. Classe
                    if (f.getClasse() != null) {
                        if (card.getClasse() == null || !card.getClasse().contains(f.getClasse())) return false;
                    }

                    // 9. Ordem
                    if (f.getOrdem() != null) {
                        if (card.getOrdem() == null || !card.getOrdem().contains(f.getOrdem())) return false;
                    }

                    // 10. Empunhadura
                    if (f.getEmpunhadura() != null && card.getEmpunhadura() != f.getEmpunhadura()) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Garante que novos jogadores (ou perfis legados sem coleção) recebam o Kit Inicial.
     */
    public void ensureStarterCollection(Player player, Deck starterPresetDeck, List<Card> catalogCards) {
        if (player == null) return;
        boolean hasHeroes = player.getOwnedHeroes() != null && !player.getOwnedHeroes().isEmpty();
        boolean hasCards = player.getOwnedCards() != null && !player.getOwnedCards().isEmpty();

        if (hasHeroes && hasCards) return;

        // 1. Concede os Heróis e Cartas do Baralho Inicial Padrão
        if (starterPresetDeck != null) {
            if (starterPresetDeck.getHeroList() != null) {
                starterPresetDeck.getHeroList().forEach(h -> player.addHeroToCollection(h.getCardName(), 1));
            }
            if (starterPresetDeck.getCardList() != null) {
                starterPresetDeck.getCardList().forEach(c -> player.addCardToCollection(c.getCardName(), 1));
            }
            if (starterPresetDeck.getLiberationList() != null) {
                starterPresetDeck.getLiberationList().forEach(l -> player.addCardToCollection(l.getCardName(), 1));
            }
        }

        // 2. Concede 2 cópias de cartas introdutórias NEUTRAL / COMUM do catálogo
        if (catalogCards != null) {
            catalogCards.stream()
                    .filter(c -> c.getColor() == com.alkateca.lendasdapoeira.enums.Colors.NEUTRAL
                              && c.getRarity() == com.alkateca.lendasdapoeira.enums.CardRarity.COMUM)
                    .forEach(c -> {
                        if (player.getOwnedCardCount(c.getCardName()) < 2) {
                            player.addCardToCollection(c.getCardName(), 2 - player.getOwnedCardCount(c.getCardName()));
                        }
                    });
        }
    }

    /**
     * Valida se todos os heróis, cartas e cartas de liberação do baralho pertencem à coleção do jogador
     * e não excedem a quantidade de cópias possuídas.
     */
    public void validateDeckOwnership(Player player, Deck deck) {
        if (player == null || deck == null) {
            throw new IllegalArgumentException("Jogador ou baralho inválido.");
        }

        // 1. Valida posse dos Heróis
        if (deck.getHeroList() != null) {
            for (HeroCard hero : deck.getHeroList()) {
                if (hero == null) continue;
                if (player.getOwnedHeroCount(hero.getCardName()) < 1) {
                    throw new IllegalArgumentException("Herói não pertence à sua coleção: " + hero.getCardName());
                }
            }
        }

        // 2. Contabiliza cópias usadas no baralho + zona de liberação e compara com ownedCards
        Map<String, Integer> requestedCopies = new HashMap<>();

        if (deck.getCardList() != null) {
            for (Card card : deck.getCardList()) {
                if (card == null) continue;
                requestedCopies.merge(card.getCardName(), 1, Integer::sum);
            }
        }

        if (deck.getLiberationList() != null) {
            for (Card libCard : deck.getLiberationList()) {
                if (libCard == null || libCard.isLiberationCard()) continue;
                requestedCopies.merge(libCard.getCardName(), 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : requestedCopies.entrySet()) {
            String cardName = entry.getKey();
            int usedCount = entry.getValue();
            int ownedCount = player.getOwnedCardCount(cardName);

            if (ownedCount <= 0) {
                throw new IllegalArgumentException("Carta não pertence à sua coleção: " + cardName);
            }
            if (usedCount > ownedCount) {
                throw new IllegalArgumentException(
                        "Quantidade insuficiente na coleção para '" + cardName + "': em uso " + usedCount + ", possuídas " + ownedCount
                );
            }
        }
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replace('_', ' ')
                .trim()
                .toUpperCase();
    }
}
