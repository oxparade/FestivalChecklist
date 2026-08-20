package fr.cecile.festivalchecklist;

import android.app.*;
import android.os.Bundle;
import android.os.Build;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    private final LinkedHashMap<String, ArrayList<Item>> data = new LinkedHashMap<>();
    private final HashMap<String, Boolean> collapsedCategories = new HashMap<>();
    private LinearLayout root;
    private TextView progress;
    private static final String PREFS = "festival_checklist";
    private static final String KEY = "data";
    private static final int COLOR_BG = Color.parseColor("#0F1115");
    private static final int COLOR_SURFACE = Color.parseColor("#171A21");
    private static final int COLOR_SURFACE_ALT = Color.parseColor("#1E2431");
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#F2F3F7");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#AEB6C6");
    private static final int COLOR_BUTTON_TEXT = Color.parseColor("#F4F6FA");
    private static final int COLOR_ACCENT = Color.parseColor("#5FB8FF");
    private static final int COLOR_DANGER = Color.parseColor("#934347");
    private static final int COLOR_BORDER = Color.parseColor("#2B3343");

    static class Item {
        String name; boolean checked;
        Item(String n, boolean c) { name = n; checked = c; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(COLOR_BG);
            getWindow().setNavigationBarColor(COLOR_BG);
        }
        load();
        render();
    }

    private void applySystemBarTextForDarkBackground() {
        View decor = getWindow().getDecorView();
        if (decor == null) return;

        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = decor.getWindowInsetsController();
            if (c != null) {
                c.setSystemBarsAppearance(0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            int flags = decor.getSystemUiVisibility();
            if (Build.VERSION.SDK_INT >= 23) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    private TextView txt(String s, int sp, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(COLOR_TEXT_PRIMARY);
        t.setPadding(18,12,18,12); if (bold) t.setTypeface(null, 1); return t;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(radiusDp));
        bg.setStroke(dp(strokeDp), stroke);
        return bg;
    }

    private Button button(String text) {
        return button(text, COLOR_SURFACE_ALT, COLOR_BUTTON_TEXT, 0, 0);
    }

    private Button button(String text, int bgColor, int textColor, int minWidthDp, int padH) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setMinHeight(dp(38));
        b.setMinimumHeight(dp(38));
        b.setMinWidth(minWidthDp == 0 ? 0 : dp(minWidthDp));
        b.setMinimumWidth(minWidthDp == 0 ? 0 : dp(minWidthDp));
        b.setPadding(dp(padH), dp(8), dp(padH), dp(8));
        b.setBackground(rounded(bgColor, COLOR_BORDER, 10, 1));
        if (Build.VERSION.SDK_INT >= 21) {
            b.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            b.setElevation(dp(1));
        }
        b.setTextColor(textColor);
        return b;
    }

    private void styleDialogInput(EditText input) {
        input.setTextColor(COLOR_TEXT_PRIMARY);
        input.setHintTextColor(COLOR_TEXT_SECONDARY);
        input.setPadding(dp(12), dp(12), dp(12), dp(12));
        input.setBackground(rounded(COLOR_SURFACE_ALT, COLOR_BORDER, 10, 1));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        final int padH = dp(12);
        final int padTop = dp(14);
        final int padBottom = dp(22);
        root.setPadding(padH, padTop, padH, padBottom);
        scroll.addView(root); setContentView(scroll);
        applySystemBarTextForDarkBackground();

        if (Build.VERSION.SDK_INT >= 21) {
            scroll.setOnApplyWindowInsetsListener((v, insets) -> {
                root.setPadding(
                        padH,
                        padTop + insets.getSystemWindowInsetTop(),
                        padH,
                        padBottom + insets.getSystemWindowInsetBottom()
                );
                return insets;
            });
            scroll.requestApplyInsets();
        }

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setBackground(rounded(COLOR_SURFACE, COLOR_BORDER, 16, 1));
        hero.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView title = new TextView(this);
        title.setText("🎪 Festival — checklist permanente");
        title.setTextColor(COLOR_TEXT_PRIMARY);
        title.setTextSize(24);
        title.setTypeface(null, 1);
        hero.addView(title);

        progress = new TextView(this);
        progress.setTextSize(14);
        progress.setTypeface(null, 1);
        progress.setTextColor(COLOR_TEXT_PRIMARY);
        progress.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(-2, -2);
        progressLp.topMargin = dp(8);
        progress.setLayoutParams(progressLp);
        hero.addView(progress);

        root.addView(hero);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, -2);
        actionsLp.topMargin = dp(12);
        actionsLp.bottomMargin = dp(8);
        actions.setLayoutParams(actionsLp);

        Button reset = button("Nouveau festival", COLOR_SURFACE_ALT, COLOR_BUTTON_TEXT, 0, 14);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Nouveau festival ?")
                .setMessage("Toutes les cases seront décochées. Ta liste restera intacte.")
                .setPositiveButton("Décoche tout", (d,w) -> { for (ArrayList<Item> l:data.values()) for(Item i:l)i.checked=false; save(); render(); })
                .setNegativeButton("Annuler", null).show());
        Button addCat = button("+ Catégorie", COLOR_ACCENT, Color.parseColor("#061622"), 0, 14);
        addCat.setOnClickListener(v -> addCategory());

        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, -2, 1);
        resetLp.rightMargin = dp(6);
        actions.addView(reset, resetLp);
        LinearLayout.LayoutParams addCatLp = new LinearLayout.LayoutParams(0, -2, 1);
        addCatLp.leftMargin = dp(6);
        actions.addView(addCat, addCatLp);
        root.addView(actions);

        for (String cat : new ArrayList<>(data.keySet())) addCategoryView(cat);
        updateProgress();
    }

    private void addCategoryView(String cat) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(COLOR_SURFACE, COLOR_BORDER, 14, 1));
        card.setPadding(dp(10), dp(10), dp(10), dp(8));
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(dp(1));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = dp(10);
        root.addView(card, cardLp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(cat);
        title.setTextColor(COLOR_TEXT_PRIMARY);
        title.setTextSize(22);
        title.setTypeface(null, 1);
        title.setPadding(dp(4), dp(2), dp(6), dp(8));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView collapse = new TextView(this);
        collapse.setText(isCollapsed(cat) ? "▸" : "▾");
        collapse.setTextColor(COLOR_TEXT_SECONDARY);
        collapse.setTextSize(20);
        collapse.setGravity(Gravity.CENTER);
        collapse.setContentDescription(isCollapsed(cat) ? "Déplier " + cat : "Replier " + cat);
        LinearLayout.LayoutParams collapseLp = new LinearLayout.LayoutParams(dp(34), dp(38));
        collapseLp.rightMargin = dp(4);
        header.addView(collapse, collapseLp);

        Button plus = button("+", COLOR_SURFACE_ALT, COLOR_BUTTON_TEXT, 36, 0);
        plus.setOnClickListener(v -> addItem(cat));
        Button del = button("×", COLOR_DANGER, COLOR_BUTTON_TEXT, 36, 0);
        del.setOnClickListener(v -> confirmDeleteCategory(cat));
        LinearLayout.LayoutParams plusLp = new LinearLayout.LayoutParams(-2, -2);
        plusLp.rightMargin = dp(6);
        header.addView(plus, plusLp);
        header.addView(del);
        card.addView(header);

        View.OnClickListener toggle = v -> {
            collapsedCategories.put(cat, !isCollapsed(cat));
            render();
        };
        header.setOnClickListener(toggle);
        title.setOnClickListener(toggle);
        collapse.setOnClickListener(toggle);

        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);
        items.setVisibility(isCollapsed(cat) ? View.GONE : View.VISIBLE);
        card.addView(items);

        for (Item item : new ArrayList<>(data.get(cat))) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(rounded(COLOR_SURFACE_ALT, COLOR_BORDER, 10, 1));
            row.setPadding(dp(8), dp(4), dp(6), dp(4));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            rowLp.topMargin = dp(6);

            CheckBox cb = new CheckBox(this);
            cb.setText(item.name);
            cb.setTextSize(15);
            cb.setChecked(item.checked);
            cb.setTextColor(COLOR_TEXT_PRIMARY);
            if (Build.VERSION.SDK_INT >= 21) cb.setButtonTintList(ColorStateList.valueOf(COLOR_ACCENT));
            cb.setPadding(dp(2), dp(0), dp(0), dp(0));
            cb.setOnCheckedChangeListener((b,c) -> { item.checked=c; save(); updateProgress(); });
            row.addView(cb, new LinearLayout.LayoutParams(0,-2,1));

            Button x = button("×", COLOR_DANGER, COLOR_BUTTON_TEXT, 34, 0);
            x.setOnClickListener(v -> { data.get(cat).remove(item); save(); render(); });
            row.addView(x);
            items.addView(row, rowLp);
        }
    }

    private boolean isCollapsed(String cat) {
        Boolean collapsed = collapsedCategories.get(cat);
        return collapsed != null && collapsed;
    }

    private void addItem(String cat) {
        final EditText input = new EditText(this); input.setHint("Ex. batterie externe");
        styleDialogInput(input);
        new AlertDialog.Builder(this).setTitle("Ajouter à “"+cat+"”").setView(input)
                .setPositiveButton("Ajouter", (d,w) -> { String s=input.getText().toString().trim(); if(!s.isEmpty()){ data.get(cat).add(new Item(s,false)); save(); render(); } })
                .setNegativeButton("Annuler",null).show();
    }

    private void addCategory() {
        final EditText input = new EditText(this); input.setHint("Ex. Maquillage / fun");
        styleDialogInput(input);
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
        if(progress!=null) {
            progress.setText("✓ " + done + " / " + total + " — " + pct + " % prêt");
            int chipColor = pct == 100 ? Color.parseColor("#2B7B48") : COLOR_SURFACE_ALT;
            progress.setBackground(rounded(chipColor, COLOR_BORDER, 999, 1));
        }
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
