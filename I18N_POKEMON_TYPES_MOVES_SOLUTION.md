# Internationalization Solution for Pokémon Types, Moves, and DB Columns

## Problem Analysis

You need to internationalize:
1. **Pokémon Types** (fire, water, grass, etc.) - stored in database and displayed in UI
2. **Move/Attack Names** (stored in `movesData.json` and `movesPokemon.json`)
3. **Database Column Names** in PokedexPanel table headers
4. Keep **Pokémon names universal** (no translation needed)

## Current Architecture

### Data Storage
- **Pokémon**: SQLite database (`pokedex` table) with `type1` and `type2` columns
- **Moves Stats**: `movesData.json` (move properties like power, accuracy, type)
- **Pokémon Moves**: `movesPokemon.json` (which moves each Pokémon knows)
- **UI Translations**: `messages*.properties` files (already has `type.*` keys)

### Current Flow
```
Database (English types) → PokemonRepository → PokemonService → PokedexPanel (displays English)
movesData.json (English) → BattleService → EnhancedBattlePanel (displays English)
```

---

## Solution: Multi-Layer Internationalization Strategy

### ✅ Solution 1: Types (EASIEST - Recommended)

**Keep types in English in the database, translate only for display**

#### Implementation Steps:

1. **Already done**: Your `messages*.properties` files have type translations:
```properties
# messages_en_US.properties
type.all=All
type.normal=Normal
type.fire=Fire
type.water=Water
...

# messages_pt_BR.properties
type.all=Todos
type.normal=Normal
type.fire=Fogo
type.water=Água
...

# messages_es_ES.properties
type.all=Todos
type.normal=Normal
type.fire=Fuego
type.water=Agua
...
```

2. **Create a TypeTranslator utility class**:

```java
// src/main/java/shared/util/TypeTranslator.java
package shared.util;

public class TypeTranslator {
    
    /**
     * Translates a Pokémon type from English (DB format) to localized text
     * @param englishType The type in English (e.g., "fire", "water")
     * @return Localized type name
     */
    public static String translate(String englishType) {
        if (englishType == null || englishType.trim().isEmpty()) {
            return "";
        }
        
        // Convert to lowercase to match property keys
        String key = "type." + englishType.toLowerCase();
        return I18n.get(key);
    }
    
    /**
     * Translates "All" or any type from DB to display
     */
    public static String translateForFilter(String type) {
        if (type == null || type.equals("All")) {
            return I18n.get("type.all");
        }
        return translate(type);
    }
    
    /**
     * Gets all types translated for combo box
     * @param englishTypes Array of English type names from DB
     * @return Array of translated type names with "All" as first option
     */
    public static String[] translateTypes(String[] englishTypes) {
        String[] translated = new String[englishTypes.length];
        for (int i = 0; i < englishTypes.length; i++) {
            translated[i] = translateForFilter(englishTypes[i]);
        }
        return translated;
    }
    
    /**
     * Reverses translation: from localized text back to English (for DB queries)
     * Needed when user selects from filter dropdown
     */
    public static String toEnglish(String localizedType) {
        if (localizedType == null || localizedType.equals(I18n.get("type.all"))) {
            return "All";
        }
        
        // Check all type keys to find match
        String[] types = {"normal", "fire", "water", "electric", "grass", "ice", 
                         "fighting", "poison", "ground", "flying", "psychic", "bug",
                         "rock", "ghost", "dragon", "dark", "steel", "fairy"};
        
        for (String type : types) {
            if (I18n.get("type." + type).equals(localizedType)) {
                return capitalize(type);
            }
        }
        
        // If not found, return as-is
        return localizedType;
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
```

3. **Modify PokedexPanel to use TypeTranslator**:

```java
// In getPokemonTypes() method - modify to translate types
private String[] getPokemonTypes() {
    try {
        String[] englishTypes = pokemonService.getAllTypes();
        return TypeTranslator.translateTypes(englishTypes);
    } catch (SQLException ex) {
        LOGGER.log(Level.SEVERE, "Erro ao carregar tipos de Pokémon", ex);
        return new String[]{I18n.get("type.all")};
    }
}

// In carregarDados() - translate types for display in table
for (Pokemon pokemon : pokemons) {
    found = true;
    Object[] row = {
        carregarIcon(pokemon.getId()),
        pokemon.getId(),
        pokemon.getName(),  // Keep name universal
        pokemon.getForm(),
        TypeTranslator.translate(pokemon.getType1()),  // TRANSLATE HERE
        TypeTranslator.translate(pokemon.getType2()),  // TRANSLATE HERE
        pokemon.getHp(),
        pokemon.getAttack(),
        pokemon.getDefense(),
        pokemon.getSpAtk(),
        pokemon.getSpDef(),
        pokemon.getSpeed(),
        pokemon.getGeneration()
    };
    tableModel.addRow(row);
}

// In applyFilters() - convert back to English for DB query
private void applyFilters() {
    String selectedLocalizedType = (String) typeFilter.getSelectedItem();
    String typeForDB = TypeTranslator.toEnglish(selectedLocalizedType);  // CONVERT BACK
    
    String idText = idField.getText().trim();
    Integer id = idText.isEmpty() ? null : PokemonUtils.isValidId(idText) ? Integer.parseInt(idText) : null;
    
    if (idText.isEmpty() || id != null) {
        carregarDados(id, typeForDB, hpSlider.getValue(), attackSlider.getValue(),
            defenseSlider.getValue(), spAtkSlider.getValue(), spDefSlider.getValue(),
            speedSlider.getValue());
        
        statusBar.setText(I18n.get("pokedex.status.filtering",
            selectedLocalizedType,  // Use localized for display
            hpSlider.getValue(),
            attackSlider.getValue(),
            defenseSlider.getValue(),
            spAtkSlider.getValue(),
            spDefSlider.getValue(),
            speedSlider.getValue()));
    } else {
        showError(I18n.get("pokedex.error.invalidId"), idField);
    }
}
```

4. **Complete the translations in all property files**:

Add to `messages_pt_BR.properties`:
```properties
type.all=Todos
type.normal=Normal
type.fire=Fogo
type.water=Água
type.electric=Elétrico
type.grass=Planta
type.ice=Gelo
type.fighting=Lutador
type.poison=Veneno
type.ground=Terra
type.flying=Voador
type.psychic=Psíquico
type.bug=Inseto
type.rock=Pedra
type.ghost=Fantasma
type.dragon=Dragão
type.dark=Sombrio
type.steel=Metálico
type.fairy=Fada
```

Add to `messages_es_ES.properties`:
```properties
type.all=Todos
type.normal=Normal
type.fire=Fuego
type.water=Agua
type.electric=Eléctrico
type.grass=Planta
type.ice=Hielo
type.fighting=Lucha
type.poison=Veneno
type.ground=Tierra
type.flying=Volador
type.psychic=Psíquico
type.bug=Bicho
type.rock=Roca
type.ghost=Fantasma
type.dragon=Dragón
type.dark=Siniestro
type.steel=Acero
type.fairy=Hada
```

Add to `messages_fr_FR.properties`:
```properties
type.all=Tous
type.normal=Normal
type.fire=Feu
type.water=Eau
type.electric=Électrik
type.grass=Plante
type.ice=Glace
type.fighting=Combat
type.poison=Poison
type.ground=Sol
type.flying=Vol
type.psychic=Psy
type.bug=Insecte
type.rock=Roche
type.ghost=Spectre
type.dragon=Dragon
type.dark=Ténèbres
type.steel=Acier
type.fairy=Fée
```

Add to `messages_it_IT.properties`:
```properties
type.all=Tutti
type.normal=Normale
type.fire=Fuoco
type.water=Acqua
type.electric=Elettro
type.grass=Erba
type.ice=Ghiaccio
type.fighting=Lotta
type.poison=Veleno
type.ground=Terra
type.flying=Volante
type.psychic=Psico
type.bug=Coleottero
type.rock=Roccia
type.ghost=Spettro
type.dragon=Drago
type.dark=Buio
type.steel=Acciaio
type.fairy=Folletto
```

---

### ✅ Solution 2: Move Names (MEDIUM Complexity)

**Keep move names in English in JSON, translate for display**

#### Implementation Steps:

1. **Add move name translations to properties files**:

```properties
# messages_en_US.properties
move.aerial_ace=Aerial Ace
move.air_slash=Air Slash
move.aqua_jet=Aqua Jet
move.aura_sphere=Aura Sphere
move.bite=Bite
move.blizzard=Blizzard
move.brave_bird=Brave Bird
move.calm_mind=Calm Mind
move.crunch=Crunch
move.dragon_dance=Dragon Dance
move.earthquake=Earthquake
move.fire_blast=Fire Blast
move.flamethrower=Flamethrower
move.hydro_pump=Hydro Pump
move.ice_beam=Ice Beam
move.psychic=Psychic
move.shadow_ball=Shadow Ball
move.surf=Surf
move.thunder=Thunder
move.thunderbolt=Thunderbolt
# ... add all moves

# messages_pt_BR.properties
move.aerial_ace=Ás Aéreo
move.air_slash=Ar Cortante
move.aqua_jet=Jato de Água
move.aura_sphere=Esfera Aural
move.bite=Mordida
move.blizzard=Nevasca
move.brave_bird=Pássaro Bravo
move.calm_mind=Mente Calma
move.crunch=Mastigação
move.dragon_dance=Dança do Dragão
move.earthquake=Terremoto
move.fire_blast=Explosão de Fogo
move.flamethrower=Lança-chamas
move.hydro_pump=Hidro Bomba
move.ice_beam=Raio de Gelo
move.psychic=Psíquico
move.shadow_ball=Bola Sombria
move.surf=Surf
move.thunder=Trovão
move.thunderbolt=Raio
# ... add all moves
```

2. **Create MoveTranslator utility class**:

```java
// src/main/java/shared/util/MoveTranslator.java
package shared.util;

public class MoveTranslator {
    
    /**
     * Translates move name from English (JSON format) to localized text
     * @param englishMoveName The move name in English (e.g., "Fire Blast")
     * @return Localized move name
     */
    public static String translate(String englishMoveName) {
        if (englishMoveName == null || englishMoveName.trim().isEmpty()) {
            return "";
        }
        
        // Convert move name to property key format
        // "Fire Blast" -> "move.fire_blast"
        String key = "move." + englishMoveName.toLowerCase()
                                              .replace(" ", "_")
                                              .replace("-", "_");
        
        String translated = I18n.get(key);
        
        // If translation not found, return original
        if (translated.equals(key)) {
            return englishMoveName;
        }
        
        return translated;
    }
    
    /**
     * Batch translate array of move names
     */
    public static String[] translateMoves(String[] englishMoves) {
        if (englishMoves == null) return new String[0];
        
        String[] translated = new String[englishMoves.length];
        for (int i = 0; i < englishMoves.length; i++) {
            translated[i] = translate(englishMoves[i]);
        }
        return translated;
    }
}
```

3. **Modify EnhancedBattlePanel to display translated move names**:

```java
// In the button creation for attacks
private void updateAttackButtons() {
    for (int i = 0; i < 4; i++) {
        if (i < playerMoves.size()) {
            Move move = playerMoves.get(i);
            String translatedName = MoveTranslator.translate(move.getName());
            String translatedType = TypeTranslator.translate(move.getType());
            
            attackButtons[i].setText(String.format(
                "<html><center>%s<br><small>%s | %d PWR</small></center></html>",
                translatedName,
                translatedType,
                move.getPower()
            ));
            attackButtons[i].setEnabled(true);
        } else {
            attackButtons[i].setText("---");
            attackButtons[i].setEnabled(false);
        }
    }
}

// In battle messages
private void displayBattleMessage(String message) {
    // If message contains move names, they will already be translated
    // by the BattleService or you can translate here
    battleMessageLabel.setText(message);
}
```

4. **Modify BattleService for translated messages** (optional):

```java
// In executeTurn method, when creating battle messages
String moveName = MoveTranslator.translate(move.getName());
String message = I18n.get("battle.message.used", pokemonName, moveName);
```

---

### ✅ Solution 3: Database Column Names (EASIEST - Already Done!)

**Your column headers are already internationalized!**

Your `createTablePane()` method already uses `I18n.get()`:

```java
String[] columns = {
    I18n.get("pokedex.table.image"),    // ✅ Already i18n
    I18n.get("pokedex.table.id"),       // ✅ Already i18n
    I18n.get("pokedex.table.name"),     // ✅ Already i18n
    I18n.get("pokedex.table.form"),     // ✅ Already i18n
    I18n.get("pokedex.table.type1"),    // ✅ Already i18n
    I18n.get("pokedex.table.type2"),    // ✅ Already i18n
    I18n.get("pokedex.table.hp"),       // ✅ Already i18n
    I18n.get("pokedex.table.attack"),   // ✅ Already i18n
    I18n.get("pokedex.table.defense"),  // ✅ Already i18n
    I18n.get("pokedex.table.spAtk"),    // ✅ Already i18n
    I18n.get("pokedex.table.spDef"),    // ✅ Already i18n
    I18n.get("pokedex.table.speed"),    // ✅ Already i18n
    I18n.get("pokedex.table.gen")       // ✅ Already i18n
};
```

**Just make sure all translations are complete** in your properties files:

```properties
# messages_pt_BR.properties
pokedex.table.image=Imagem
pokedex.table.id=ID
pokedex.table.name=Nome
pokedex.table.form=Forma
pokedex.table.type1=Tipo1
pokedex.table.type2=Tipo2
pokedex.table.hp=PS
pokedex.table.attack=Ataque
pokedex.table.defense=Defesa
pokedex.table.spAtk=AtqEsp
pokedex.table.spDef=DefEsp
pokedex.table.speed=Velocidade
pokedex.table.gen=Ger

# messages_es_ES.properties
pokedex.table.image=Imagen
pokedex.table.id=ID
pokedex.table.name=Nombre
pokedex.table.form=Forma
pokedex.table.type1=Tipo1
pokedex.table.type2=Tipo2
pokedex.table.hp=PS
pokedex.table.attack=Ataque
pokedex.table.defense=Defensa
pokedex.table.spAtk=AtEsp
pokedex.table.spDef=DefEsp
pokedex.table.speed=Velocidad
pokedex.table.gen=Gen
```

---

## Summary: What to Do

### Priority 1: Types (MUST DO)
1. ✅ Create `TypeTranslator.java` utility class
2. ✅ Modify `PokedexPanel.java` to use `TypeTranslator`
3. ✅ Complete type translations in all `.properties` files
4. ✅ Test type filter and table display in all languages

### Priority 2: Move Names (RECOMMENDED)
1. ✅ Create `MoveTranslator.java` utility class
2. ✅ Add all move translations to `.properties` files (~200 moves)
3. ✅ Modify `EnhancedBattlePanel.java` to display translated names
4. ✅ Optionally modify `BattleService.java` for battle messages

### Priority 3: Column Headers (ALREADY DONE!)
1. ✅ Verify all translations exist in `.properties` files
2. ✅ No code changes needed!

---

## Files to Create/Modify

### New Files:
1. `src/main/java/shared/util/TypeTranslator.java` (new)
2. `src/main/java/shared/util/MoveTranslator.java` (new)

### Modified Files:
1. `src/main/java/frontend/view/PokedexPanel.java`
   - `getPokemonTypes()` method
   - `carregarDados()` method
   - `applyFilters()` method

2. `src/main/java/frontend/view/EnhancedBattlePanel.java`
   - `updateAttackButtons()` method (if exists)
   - Any method displaying move names

3. All `.properties` files:
   - `messages_en_US.properties`
   - `messages_pt_BR.properties`
   - `messages_es_ES.properties`
   - `messages_fr_FR.properties`
   - `messages_it_IT.properties`

---

## Advantages of This Solution

✅ **No database changes required** - types stay in English in DB  
✅ **No JSON changes required** - moves stay in English in JSON  
✅ **Separation of concerns** - data layer vs presentation layer  
✅ **Easy to add new languages** - just add new properties file  
✅ **Pokémon names universal** - as requested  
✅ **Consistent with existing i18n pattern** - uses I18n class  
✅ **Type-safe** - utility classes handle translation logic  
✅ **Backward compatible** - doesn't break existing functionality  

---

## Testing Checklist

- [ ] Type filter shows translated types in dropdown
- [ ] Table displays translated types in Type1/Type2 columns
- [ ] Filtering by type works correctly (converts back to English)
- [ ] Status bar shows translated type in filter message
- [ ] Battle panel shows translated move names on buttons
- [ ] Battle messages show translated move names
- [ ] All translations work in all 5 languages
- [ ] Pokémon names remain universal (not translated)
- [ ] Column headers display correctly in all languages

---

## Need Help?

If you want me to:
1. Generate the complete translation files with all moves
2. Implement the TypeTranslator class
3. Modify the PokedexPanel.java file
4. Create automated tests

Just ask! 🚀
