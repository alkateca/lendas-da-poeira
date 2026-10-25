package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.ShopDtos.PurchaseResponseDTO;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardRarity;
import com.alkateca.lendasdapoeira.enums.CardSet;
import com.alkateca.lendasdapoeira.enums.CardType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShopAndBoosterIntegrationTest {

    @Test
    void testBoosterPurchaseGeneratesFiveCardsAndRespectsBalance() {
        // Arrange (Preparação)
        OpenBoosterService openBoosterService = new OpenBoosterService();

        Card cComum = new Card();
        cComum.setCardName("Ataque Mágico");
        cComum.setCardType(CardType.SPELL);
        cComum.setRarity(CardRarity.COMUM);
        cComum.setCardSet(CardSet.BASE);

        Card cRara = new Card();
        cRara.setCardName("Massacre Cristalino");
        cRara.setCardType(CardType.ACTION);
        cRara.setRarity(CardRarity.RARA);
        cRara.setCardSet(CardSet.BASE);

        HeroCard hLendario = new HeroCard();
        hLendario.setCardName("Avatar de Poeira");
        hLendario.setRarity(CardRarity.LENDARIA);
        hLendario.setCardSet(CardSet.BASE);

        openBoosterService.setCatalogsForTesting(List.of(cComum, cRara), List.of(hLendario));
        ShopService shopService = new ShopService(openBoosterService, new PresetDeckService());

        Player player = new Player(UUID.randomUUID(), "CompradorTeste", null);
        player.setPoeira(150); // Custo do booster-base-5 é 100

        // Act 1 (Ação: Comprar e abrir 1 Booster de 100 Poeira)
        PurchaseResponseDTO response = shopService.purchaseProduct(player, "booster-base-5");

        // Assert 1 (Validação de Saldo, Quantidade de Cartas e Vínculo na Coleção)
        assertEquals(50, player.getPoeira(), "O saldo deve diminuir de 150 para 50 após a compra.");
        assertEquals(5, response.getOpenedBoosterCards().size(), "O booster deve gerar exatamente 5 cartas.");

        int totalCopiesInCollection = player.getOwnedCards().values().stream().mapToInt(Integer::intValue).sum()
                + player.getOwnedHeroes().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(5, totalCopiesInCollection, "Todas as 5 cartas sorteadas devem ser persistidas na coleção do jogador.");

        // Act & Assert 2 (Tentativa de comprar novamente com apenas 50 de Poeira)
        assertThrows(IllegalStateException.class, () -> {
            shopService.purchaseProduct(player, "booster-base-5");
        }, "Deve bloquear a compra quando o jogador não possui saldo suficiente.");
    }
}
