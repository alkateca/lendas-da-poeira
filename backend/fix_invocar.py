import json

def fix_invocar(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        data = json.load(f)
        
    for item in data:
        desc = item.get('descricao', '')
        
        # cards.json
        if "Crie e Jogue 3 Bolas de Fogo" in desc:
            desc = desc.replace("Crie e Jogue 3 Bolas de Fogo", "Invocar: Bola de Fogo (3x)") # How to format 3? Let's just use "Invocar: Bola de Fogo" and maybe indicate 3 somehow, or just "Invocar: Bola de Fogo\nInvocar: Bola de Fogo\nInvocar: Bola de Fogo". "Invocar 3x: Bola de Fogo"? The rule says "Invocar: [Nome Exato da Carta]". I'll use "Invocar: Bola de Fogo" 3 vezes or "Invocar: Bola de Fogo". Let's do "Invocar: Bola de Fogo" (if it implies 3, wait, the text says 3. I will replace it with "Invocar: Bola de Fogo. Invocar: Bola de Fogo. Invocar: Bola de Fogo.")
            desc = desc.replace("Invocar: Bola de Fogo (3x)", "Invocar: Bola de Fogo\nInvocar: Bola de Fogo\nInvocar: Bola de Fogo")
            
        if "Crie uma Estatica em seu baralho" in desc:
            desc = desc.replace("Crie uma Estatica em seu baralho", "Invocar: Estatica")
        if "Crie e Jogue uma Estatica" in desc:
            desc = desc.replace("Crie e Jogue uma Estatica", "Invocar: Estatica")
        
        # heros.json
        if "Crie e Jogue uma Ritos Fúnebres" in desc:
            desc = desc.replace("Crie e Jogue uma Ritos Fúnebres.", "Invocar: Ritos Fúnebres.")
        
        if "Jogue um Vendaval Arcano e uma Barreira de Gelo" in desc:
            desc = desc.replace("Jogue um Vendaval Arcano e uma Barreira de Gelo.", "Invocar: Vendaval Arcano\nInvocar: Barreira de Gelo.")
            
        if "Jogue Dragast de sua mão, baralho ou descarte" in desc:
            desc = desc.replace("Jogue Dragast de sua mão, baralho ou descarte", "Invocar: Dragast")
            
        if "Se houver uma Fortaleça sua Fé em sua mão, a jogue" in desc:
            desc = desc.replace("Se houver uma Fortaleça sua Fé em sua mão, a jogue", "Invocar: Fortaleça sua Fé")
            
        if "Se houver uma Bola de Fogo em sua mão, a jogue" in desc:
            desc = desc.replace("Se houver uma Bola de Fogo em sua mão, a jogue", "Invocar: Bola de Fogo")
            
        if "Jogue uma Bola de Fogo de seu baralho ou mão" in desc:
            desc = desc.replace("Jogue uma Bola de Fogo de seu baralho ou mão", "Invocar: Bola de Fogo")
            
        item['descricao'] = desc

    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

fix_invocar('src/main/resources/cards.json')
fix_invocar('src/main/resources/heros.json')
print("Fixed!")
