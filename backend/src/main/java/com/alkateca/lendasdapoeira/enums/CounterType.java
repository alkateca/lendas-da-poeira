package com.alkateca.lendasdapoeira.enums;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Tipos de contadores (marcadores numéricos) aplicáveis sobre cartas em jogo.
 * Novos contadores podem ser adicionados aqui com seus metadados visuais e descritivos
 * sem exigir alterações estruturais no motor ou no Front-End.
 */
public enum CounterType {
    POEIRA(
            "Poeira",
            "counter_poeira",
            "#facc15",
            "Marcador de Poeira acumulado sobre a carta."
    ),
    VENENO(
            "Veneno",
            "counter_veneno",
            "#22c55e",
            "Marcador de Veneno acumulado sobre a carta."
    ),
    POLVORA(
            "Pólvora",
            "counter_polvora",
            "#ef4444",
            "Marcador de Pólvora acumulado sobre a carta."
    ),
    COMBUSTAO(
            "Combustão",
            "counter_combustao",
            "#f97316",
            "Adiciona o valor total de Combustão ao dano causado apenas por Magias de Fogo contra o portador e zera esses contadores."
    );

    private final String displayName;
    private final String iconKey;
    private final String accentColor;
    private final String description;

    CounterType(String displayName, String iconKey, String accentColor, String description) {
        this.displayName = displayName;
        this.iconKey = iconKey;
        this.accentColor = accentColor;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconKey() {
        return iconKey;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolve um tipo de contador a partir do nome do enum, rótulo exibido ou chave de ícone,
     * ignorando acentos e maiúsculas/minúsculas.
     */
    public static CounterType fromKeyOrLabel(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalizedInput = normalizeToken(rawValue);
        for (CounterType type : values()) {
            if (normalizeToken(type.name()).equals(normalizedInput)
                    || normalizeToken(type.displayName).equals(normalizedInput)
                    || normalizeToken(type.iconKey).equals(normalizedInput)) {
                return type;
            }
        }
        return null;
    }

    private static String normalizeToken(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
