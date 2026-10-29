package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.ItemType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeywordExtractor {

    private static final Map<String, String> TERM_PATTERNS = new LinkedHashMap<>();

    static {
        // Ações e Gatilhos
        TERM_PATTERNS.put("INVOCAR", "invocar:|invoque:");
        TERM_PATTERNS.put("AURA", "aura:");
        TERM_PATTERNS.put("INICIO_DA_PARTIDA", "inicio da partida:");
        TERM_PATTERNS.put("INICIO_DO_COMBATE", "inicio do combate:|inicio de combate");
        TERM_PATTERNS.put("FINAL_DO_COMBATE", "final do combate:");
        TERM_PATTERNS.put("UMA_VEZ_POR_COMBATE", "uma vez por combate:");
        TERM_PATTERNS.put("AO_EQUIPAR", "ao equipar:|enquanto estiver equipada:");

        // Tipos de Dano e Combate
        TERM_PATTERNS.put("DANO_DIRETO", "dano direto");
        TERM_PATTERNS.put("DANO_MAGICO", "dano magico");
        TERM_PATTERNS.put("DANO_FISICO", "dano fisico|ataque fisico");
        TERM_PATTERNS.put("DANO_BONUS", "dano bonus");
        TERM_PATTERNS.put("REDUCAO_DE_DANO", "reducao de dano");
        TERM_PATTERNS.put("VULNERABILIDADE", "vulnerabilidade");
        TERM_PATTERNS.put("EMPUNHADURA", "empunhadura");

        // Estados de Jogo e Zonas
        TERM_PATTERNS.put("FERIDO", "ferido");
        TERM_PATTERNS.put("EXILAR", "exilar|exile");
        TERM_PATTERNS.put("MORTO", "morto|morre|reviva");
        TERM_PATTERNS.put("ATIVO", "estado ativo");

        // Estruturas de Turno
        TERM_PATTERNS.put("FASE", "fase de");
        TERM_PATTERNS.put("RESOLUCAO", "resolucao");
    }

    public static List<String> extract(Card card) {
        Set<String> result = new LinkedHashSet<>();
        if (card == null) return new ArrayList<>();

        if (card.getKeywords() != null) {
            result.addAll(card.getKeywords());
        }

        if (card.getItemType() == ItemType.ARMA && card.getEmpunhadura() > 0) {
            result.add("EMPUNHADURA");
        }

        if (card instanceof HeroCard hero) {
            if (hero.getDanoBonus() > 0) result.add("DANO_BONUS");
            if (hero.getReducaoDano() > 0) result.add("REDUCAO_DE_DANO");
            if (hero.getVulnerabilidade() > 0) result.add("VULNERABILIDADE");
        }

        String rawDesc = card.getDescricao();
        if (rawDesc != null && !rawDesc.isBlank()) {
            String normalized = normalize(rawDesc);
            for (Map.Entry<String, String> entry : TERM_PATTERNS.entrySet()) {
                String[] alternatives = entry.getValue().split("\\|");
                for (String token : alternatives) {
                    if (normalized.contains(token)) {
                        result.add(entry.getKey());
                        break;
                    }
                }
            }
        }

        return new ArrayList<>(result);
    }

    private static String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase();
    }
}
