# 🧪 Testing Guide - Type Internationalization

## Quick Testing Steps

### 1️⃣ Test PokedexPanel - Type Filter & Table

1. **Run the application**
   ```bash
   mvn clean compile exec:java
   ```

2. **Select Language**
   - In WelcomeFrame, select different languages
   - Notice the button text changes

3. **Login** and open PokedexPanel

4. **Test Type Filter Dropdown**
   - Click on the Type dropdown
   - **Expected**: Should see translated types
     - English: "All, Normal, Fire, Water..."
     - Portuguese: "Todos, Normal, Fogo, Água..."
     - Spanish: "Todos, Normal, Fuego, Agua..."
     - French: "Tous, Normal, Feu, Eau..."
     - Italian: "Tutti, Normale, Fuoco, Acqua..."

5. **Test Type Filtering**
   - Select "Fire" (or "Fogo" in Portuguese, "Fuego" in Spanish, etc.)
   - **Expected**: Table should show only Fire-type Pokémon
   - **Verify**: Type1/Type2 columns show translated type names

6. **Test Table Display**
   - Look at Type1 and Type2 columns
   - **Expected**: Types are in your selected language
   - Example row (Portuguese):
     - Charizard: Fogo / Voador
     - Pikachu: Elétrico / (empty)

7. **Test Status Bar**
   - After filtering, check the status bar at bottom
   - **Expected**: Should show "Filtering by Type: Fogo..." (in Portuguese)

### 2️⃣ Test EnhancedBattlePanel - Type Badges

1. **Start a Battle**
   - From PokedexPanel, click "Battle"
   - Select 5 Pokémon for your team
   - Click "Start Battle"

2. **Check Active Pokémon Type Badges**
   - Look at the type badges below Pokémon names
   - **Expected**: Types shown in current language
     - English: "FIRE / FLYING"
     - Portuguese: "FOGO / VOADOR"
     - Spanish: "FUEGO / VOLADOR"
     - French: "FEU / VOL"
     - Italian: "FUOCO / VOLANTE"

3. **Check Attack Buttons**
   - Look at the 4 attack buttons
   - **Expected**: Type shown in current language
     - Example: "Fire Blast - PWR: 110 | FOGO" (Portuguese)

4. **Test Pokémon Switching**
   - Click "Switch Pokémon"
   - Select a different Pokémon
   - **Expected**: Type badges update with translated types

### 3️⃣ Test Language Switching

1. **During Session**
   - Exit to login screen
   - Go back to WelcomeFrame
   - Change language
   - Open PokedexPanel again

2. **Verify Updates**
   - Type dropdown shows new language
   - Table Type1/Type2 columns show new language
   - Battle type badges show new language

### 4️⃣ Edge Cases to Test

1. **Pokémon with Single Type**
   - Example: Pikachu (Electric only)
   - **Expected**: Type2 column is empty, no second badge

2. **Pokémon with Dual Types**
   - Example: Charizard (Fire/Flying)
   - **Expected**: Both types translated correctly

3. **Special Characters**
   - Portuguese: "Água" (water), "Dragão" (dragon)
   - French: "Électrik", "Ténèbres"
   - **Expected**: Special characters display correctly

4. **Type with Same Name**
   - "Normal" is same in most languages
   - "Dragon" is "Dragão" (PT), "Dragón" (ES), "Dragon" (EN/FR)
   - **Expected**: Shows correctly in each language

---

## 🐛 What to Look For (Potential Issues)

### ❌ If Types Don't Translate
- **Symptom**: Types still show in English
- **Check**: 
  1. Language was changed
  2. Application was restarted
  3. TypeTranslator is being called

### ❌ If Filtering Doesn't Work
- **Symptom**: Selecting type doesn't filter
- **Check**: 
  1. TypeTranslator.toEnglish() is working
  2. Database query receives English type name

### ❌ If Type Colors Are Wrong
- **Symptom**: Type badge has wrong color
- **Check**: 
  1. PokemonUtils.getTypeColor() receives original English type
  2. Not the translated type

---

## ✅ Expected Results Summary

| Component | What to See |
|-----------|-------------|
| **Type Dropdown** | Translated types (Fogo, Fuego, Feu, etc.) |
| **Table Type1/Type2** | Translated types in columns |
| **Status Bar** | Filter message with translated type |
| **Battle Type Badges** | Translated type names |
| **Attack Buttons** | Move type in current language |

---

## 📸 Visual Checklist

### PokedexPanel
- [ ] Type dropdown has translated options
- [ ] Selecting a type filters correctly
- [ ] Type1 column shows translated types
- [ ] Type2 column shows translated types
- [ ] Status bar shows translated type in filter message

### BattlePanel
- [ ] Active Pokémon card shows translated type badges
- [ ] Attack buttons show translated move types
- [ ] After switching Pokémon, new types are translated
- [ ] Both player and enemy Pokémon show translated types

### All Languages
- [ ] English (en_US) ✓
- [ ] Portuguese (pt_BR) ✓
- [ ] Spanish (es_ES) ✓
- [ ] French (fr_FR) ✓
- [ ] Italian (it_IT) ✓

---

## 🎉 Success!

If all tests pass, congratulations! Your Pokémon application now fully supports internationalized type names across all components!

**Next**: If you want to add Move name translations (Phase 2), let me know! 🚀
