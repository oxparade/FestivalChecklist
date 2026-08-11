package fr.cecile.festivalchecklist;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    private final LinkedHashMap<String, ArrayList<Item>> data = new LinkedHashMap<>();
    private LinearLayout root;
    private TextView progress;
    private static final String PREFS = "festival_checklist";
    private static final String KEY = "data";

    static class Item {
        String name; boolean checked;
        Item(String n, boolean c) { name = n; checked = c; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        load();
        render();
    }

    private TextView txt(String s, int sp, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.rgb(30,30,30));
        t.setPadding(18,12,18,12); if (bold) t.setTypeface(null, 1); return t;
    }

    private Button button(String text) {
        Button b = new Button(this); b.setText(text); return b;
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(14,20,14,30);
        scroll.addView(root); setContentView(scroll);

        root.addView(txt("🎪 Festival — checklist permanente", 24, true));
        progress = txt("", 16, true); root.addView(progress);

        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button reset = button("Nouveau festival");
        reset.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Nouveau festival ?")
                .setMessage("Toutes les cases seront décochées. Ta liste restera intacte.")
                .setPositiveButton("Décoche tout", (d,w) -> { for (ArrayList<Item> l:data.values()) for(Item i:l)i.checked=false; save(); render(); })
                .setNegativeButton("Annuler", null).show());
        Button addCat = button("+ Catégorie"); addCat.setOnClickListener(v -> addCategory());
        actions.addView(reset, new LinearLayout.LayoutParams(0,-2,1)); actions.addView(addCat, new LinearLayout.LayoutParams(0,-2,1)); root.addView(actions);

        for (String cat : new ArrayList<>(data.keySet())) addCategoryView(cat);
        updateProgress();
    }

    private void addCategoryView(String cat) {
        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = txt(cat, 20, true); header.addView(title, new LinearLayout.LayoutParams(0,-2,1));
        Button plus = button("+"); plus.setOnClickListener(v -> addItem(cat)); header.addView(plus);
        Button del = button("×"); del.setOnClickListener(v -> confirmDeleteCategory(cat)); header.addView(del);
        root.addView(header);

        for (Item item : new ArrayList<>(data.get(cat))) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            CheckBox cb = new CheckBox(this); cb.setText(item.name); cb.setTextSize(16); cb.setChecked(item.checked);
            cb.setOnCheckedChangeListener((b,c) -> { item.checked=c; save(); updateProgress(); });
            row.addView(cb, new LinearLayout.LayoutParams(0,-2,1));
            Button x = button("×"); x.setOnClickListener(v -> { data.get(cat).remove(item); save(); render(); }); row.addView(x);
            root.addView(row);
        }
    }

    private void addItem(String cat) {
        final EditText input = new EditText(this); input.setHint("Ex. batterie externe");
        new AlertDialog.Builder(this).setTitle("Ajouter à “"+cat+"”").setView(input)
                .setPositiveButton("Ajouter", (d,w) -> { String s=input.getText().toString().trim(); if(!s.isEmpty()){ data.get(cat).add(new Item(s,false)); save(); render(); } })
                .setNegativeButton("Annuler",null).show();
    }

    private void addCategory() {
        final EditText input = new EditText(this); input.setHint("Ex. Maquillage / fun");
        new AlertDialog.Builder(this).setTitle("Nouvelle catégorie").setView(input)
                .setPositiveButton("Ajouter", (d,w) -> { String s=input.getText().toString().trim(); if(!s.isEmpty() && !data.containsKey(s)){ data.put(s,new ArrayList<>()); save(); render(); } })
                .setNegativeButton("Annuler",null).show();
    }

    private void confirmDeleteCategory(String cat) {
        new AlertDialog.Builder(this).setTitle("Supprimer “"+cat+"” ?").setMessage("La catégorie et tous ses éléments seront supprimés.")
                .setPositiveButton("Supprimer", (d,w) -> { data.remove(cat); save(); render(); }).setNegativeButton("Annuler",null).show();
    }

    private void updateProgress() {
        int total=0, done=0; for(ArrayList<Item> l:data.values()) for(Item i:l){ total++; if(i.checked)done++; }
        int pct = total==0?0:(int)Math.round(done*100.0/total);
        if(progress!=null) progress.setText("✓ "+done+" / "+total+" — "+pct+" % prêt");
    }

    private void defaults() {
        add("⛺ Campement", "Tente + sardines + maillet", "Matelas / tapis de sol", "Sac de couchage léger", "Petit oreiller", "Plaid léger", "Bâche sous la tente", "Lampe frontale", "Chaise pliante", "Bouchons d’oreilles pour dormir", "Masque de nuit", "Sacs-poubelle / sacs étanches");
        add("👕 Vêtements", "5 culottes", "5 paires de chaussettes", "4 hauts légers", "2 bas légers", "1 pantalon confortable", "Pyjama", "Sweat / polaire", "Veste imperméable / poncho", "Casquette / chapeau", "Lunettes de soleil", "Chaussures confortables", "Deuxième paire / sandales", "Tenue complète de secours au sec");
        add("🚿 Hygiène", "Brosse à dents + dentifrice", "Déodorant", "Gel douche / savon", "Shampoing", "Brosse / peigne + élastiques", "Serviette microfibre", "Gant de toilette", "Papier toilette qui ne peluche pas", "Mouchoirs", "Lingettes sans parfum", "Gel hydroalcoolique", "Petit miroir", "Sac pour linge sale", "Protections périodiques de secours");
        add("☀️ Chaleur / peau / bobos", "Crème solaire SPF 50+", "Stick solaire visage / lèvres", "Gourde", "Brumisateur", "Éventail", "Crème barrière habituelle", "Pansements", "Pansements ampoules", "Désinfectant", "Antalgique habituel", "Médicaments personnels", "Répulsif moustiques", "Tire-tique", "Pince à épiler");
        add("🔋 Téléphone / tech", "Téléphone", "Chargeur", "2 câbles", "Batterie externe 20 000 mAh", "Petite batterie externe", "Écouteurs", "Multiprise compacte", "Billets / QR codes hors ligne", "Carte / adresse du festival hors ligne");
        add("🪪 Indispensables", "Carte d’identité", "Carte bancaire", "Espèces", "Carte Vitale", "Billet / pass festival", "Clés maison", "Clés voiture", "Permis", "Copie des infos importantes ailleurs que sur le téléphone");
        add("🥤 Bouffe / hydratation", "Gourde", "Réserve d’eau au camping", "Barres céréales", "Fruits secs / noix", "Compotes", "Biscuits / crackers", "Snacks salés", "Petit-déjeuner x4", "Café / thé", "Gobelet réutilisable", "Couverts");
        add("🎶 Petit sac festival", "Téléphone", "Batterie", "Gourde", "Lunettes", "Crème solaire", "Bouchons d’oreilles de concert", "Mouchoirs / PQ", "Gel hydroalcoolique", "Carte bancaire", "Veste de pluie compacte", "Pansements", "Éventail");
        add("✨ Petits trucs utiles", "Mousquetons", "Ficelle", "Pinces à linge", "Sacs zip étanches", "PQ dans sac zip", "Petite lampe dans la tente", "Télécharger programmation / carte", "Repérer eau, toilettes, secours et sortie", "Signe distinctif sur la tente");
    }

    private void add(String cat, String... items) {
        ArrayList<Item> l=new ArrayList<>(); for(String s:items)l.add(new Item(s,false)); data.put(cat,l);
    }

    private void save() {
        try {
            JSONObject o=new JSONObject(); for(String cat:data.keySet()) { JSONArray a=new JSONArray(); for(Item i:data.get(cat)){ JSONObject x=new JSONObject(); x.put("name",i.name); x.put("checked",i.checked); a.put(x);} o.put(cat,a); }
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY,o.toString()).apply();
        } catch(Exception ignored){}
    }

    private void load() {
        String raw=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY,null); if(raw==null){ defaults(); save(); return; }
        try { JSONObject o=new JSONObject(raw); Iterator<String> keys=o.keys(); while(keys.hasNext()){ String cat=keys.next(); JSONArray a=o.getJSONArray(cat); ArrayList<Item> l=new ArrayList<>(); for(int j=0;j<a.length();j++){ JSONObject x=a.getJSONObject(j); l.add(new Item(x.getString("name"),x.optBoolean("checked",false))); } data.put(cat,l);} }
        catch(Exception e){ data.clear(); defaults(); save(); }
    }
}
