package tz.mengidata.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

final class LocaleText {
    private static final String PREFS = "mengi_data_preferences";
    private static final String LANGUAGE = "language";
    private static final Map<String, String> EN = new HashMap<>();
    private static volatile boolean english;

    static {
        String[][] pairs = {
                {"Dashibodi", "Dashboard"}, {"Miamala", "Transactions"}, {"Ankara", "Invoices"},
                {"Bili", "Bills"}, {"Ripoti", "Reports"}, {"Daftari", "Notebook"},
                {"Daftari la biashara", "Business notebook"}, {"Mipangilio", "Settings"},
                {"Muonekano", "Appearance"},
                {"Chagua mandhari ya kufuata simu, mwanga, au giza halisi lenye mandharinyuma meusi.",
                        "Choose your phone's theme, light, or true dark with black backgrounds."},
                {"Kurasa zote", "All pages"}, {"Zilizowekwa alama", "Bookmarked"},
                {"kurasa zimetumika", "pages used"}, {"UKURASA ", "PAGE "},
                {"Ondoa alama ya ukurasa", "Remove page bookmark"},
                {"Weka alama ya ukurasa", "Bookmark page"},
                {"Ukurasa mpya", "New page"}, {"Hariri ukurasa", "Edit page"},
                {"KICHWA *", "TITLE *"}, {"Mfano: Oda ya mteja", "Example: Customer order"},
                {"Andika dokezo lako la biashara hapa", "Write your business note here"},
                {"Futa ukurasa", "Delete page"}, {"Futa ukurasa?", "Delete page?"},
                {"Dokezo hili litafutwa kabisa.", "This note will be deleted permanently."},
                {"Weka kichwa cha ukurasa.", "Enter a page title."},
                {"Daftari limefika kikomo cha kurasa 500.", "The notebook has reached its 500-page limit."},
                {"Bado hakuna ukurasa. Bonyeza + kuandika dokezo la kwanza.",
                        "No pages yet. Tap + to write your first business note."},
                {"Hakuna ukurasa uliowekwa alama bado.", "No bookmarked pages yet."},
                {"Ankara ", "Invoice "}, {"Ankara", "Invoice"}, {"Ankara zote", "All invoices"},
                {"bili", "bills"}, {"Mwezi huu", "This month"},
                {"Muhtasari wa biashara", "Business overview"}, {"Fedha zako, kwa mpangilio.", "Your finances, organized."},
                {"Muhtasari wa biashara yako, hata bila intaneti.", "Your business overview, even offline."},
                {"Mwezi huu", "This month"}, {"Fedha zilizoingia", "Income"}, {"Fedha zilizotoka", "Expenses"},
                {"Faida / hasara", "Profit / loss"}, {"Miamala ya hivi karibuni", "Recent transactions"},
                {"Bado hakuna miamala. Bonyeza + kuongeza ya kwanza.", "No transactions yet. Tap + to add your first."},
                {"Mwenendo wa miezi 6", "6-month trend"}, {"Bili ambazo hazijalipwa", "Unpaid bills"},
                {"Fungua bili", "Open bills"}, {"Mwezi huu", "This month"},
                {"Mapato na matumizi", "Income and expenses"}, {"Ankara na malipo ya wateja", "Invoices and customer payments"},
                {"Bili na malipo yanayokuja", "Bills and upcoming payments"}, {"Muhtasari wa fedha", "Financial summary"},
                {"Biashara, ankara na data", "Business, invoices and data"},
                {"Tafuta na chuja", "Search and filter"}, {"Tafuta maelezo au mteja", "Search description or customer"},
                {"Zote", "All"}, {"Mapato", "Income"}, {"Matumizi", "Expenses"},
                {"AINA", "TYPE"}, {"KUNDI", "CATEGORY"}, {"KUMBUKUMBU", "REFERENCE"},
                {"NJIA YA MALIPO", "PAYMENT METHOD"}, {"MTEJA / MSAMBAZAJI", "CUSTOMER / SUPPLIER"},
                {"MALIPO", "PAYMENT"}, {"MAELEZO", "NOTES"},
                {"Kitendo hiki hakiwezi kutenduliwa.", "This action cannot be undone."},
                {"Hakuna miamala inayolingana na utafutaji.", "No transactions match your search."},
                {"Fedha ndani (mapato)", "Money in (income)"}, {"Fedha nje (matumizi)", "Money out (expense)"},
                {"TAREHE (YYYY-MM-DD)", "DATE (YYYY-MM-DD)"}, {"MAELEZO", "DESCRIPTION"},
                {"Mfano: Mauzo ya bidhaa", "Example: Product sale"}, {"MTEJA / MSAMBAZAJI", "CUSTOMER / SUPPLIER"},
                {"KUNDI", "CATEGORY"}, {"KIASI (TZS)", "AMOUNT (TZS)"}, {"NJIA YA MALIPO", "PAYMENT METHOD"},
                {"Cash, M-Pesa, Benki", "Cash, M-Pesa, Bank"}, {"KUMBUKUMBU", "REFERENCE"},
                {"MAELEZO YA ZIADA", "ADDITIONAL NOTES"}, {"Ongeza muamala", "Add transaction"},
                {"Hariri muamala", "Edit transaction"}, {"Ghairi", "Cancel"}, {"Hifadhi", "Save"},
                {"Weka maelezo, kiasi sahihi na tarehe ya YYYY-MM-DD.", "Enter a description, valid amount, and date in YYYY-MM-DD format."},
                {"Ankara zote", "All invoices"}, {"Bado hakuna ankara. Bonyeza + kuunda ankara.", "No invoices yet. Tap + to create an invoice."},
                {"Angalia", "View"}, {"Hariri", "Edit"}, {"Futa", "Delete"}, {"Lipwa", "Paid"},
                {"Chapisha", "Print"}, {"PDF", "PDF"}, {"Sawa", "OK"}, {"Mteja", "Customer"},
                {"Tarehe", "Date"}, {"Mwisho wa malipo", "Due date"}, {"Jumla", "Total"},
                {"Imelipwa", "Paid"}, {"Salio", "Balance"}, {"Hali", "Status"},
                {"Tengeneza ankara", "Create invoice"}, {"JINA LA MTEJA *", "CUSTOMER NAME *"},
                {"NAMBA YA ANKARA", "INVOICE NUMBER"}, {"SIMU", "PHONE"}, {"BARUA PEPE", "EMAIL"},
                {"ANWANI", "ADDRESS"}, {"TAREHE YA KUTOA (YYYY-MM-DD)", "ISSUE DATE (YYYY-MM-DD)"},
                {"TAREHE YA MALIPO (YYYY-MM-DD)", "DUE DATE (YYYY-MM-DD)"}, {"PUNGUZO (TZS)", "DISCOUNT (TZS)"},
                {"KIASI KILICHOLIPWA", "AMOUNT PAID"}, {"UJUMBE KWENYE ANKARA", "INVOICE NOTE"},
                {"BIDHAA / HUDUMA", "PRODUCTS / SERVICES"}, {"MAELEZO", "DESCRIPTION"},
                {"Bidhaa au huduma", "Product or service"}, {"IDADI", "QUANTITY"}, {"BEI (TZS)", "PRICE (TZS)"},
                {"Ondoa bidhaa", "Remove item"}, {"Ankara inahitaji angalau bidhaa moja.", "An invoice needs at least one item."},
                {"＋ Ongeza bidhaa/huduma", "＋ Add product/service"}, {"Hifadhi rasimu", "Save draft"},
                {"Hifadhi ankara", "Save invoice"}, {"Weka jina, namba ya ankara na tarehe sahihi.", "Enter a name, invoice number, and valid dates."},
                {"Namba hii ya ankara tayari inatumika.", "This invoice number is already in use."},
                {"Ongeza angalau bidhaa au huduma.", "Add at least one product or service."},
                {"Futa ankara?", "Delete invoice?"}, {"Weka imelipwa?", "Mark as paid?"},
                {"Ankara haijalipwa", "Invoice unpaid"}, {"Kushiriki ankara", "Share invoice"},
                {"Shiriki", "Share"}, {"Hifadhi PDF", "Save PDF"}, {"Chapisha ankara", "Print invoice"},
                {"Jina la bili", "Bill name"}, {"Bili zote", "All bills"},
                {"Bado hakuna bili. Bonyeza + kuongeza bili.", "No bills yet. Tap + to add a bill."},
                {"Weka haijalipwa", "Mark unpaid"}, {"Weka imelipwa", "Mark paid"},
                {"Inasubiri", "Pending"}, {"Imechelewa", "Overdue"}, {"Ongeza bili", "Add bill"},
                {"Hariri bili", "Edit bill"}, {"JINA LA BILI *", "BILL NAME *"}, {"MLIPWAJI", "PAYEE"},
                {"Kodi ya pango", "Rent"}, {"Umeme", "Electricity"}, {"Maji", "Water"},
                {"Umeme & maji", "Utilities"}, {"Internet & simu", "Internet & phone"},
                {"Chakula", "Food"}, {"Malipo ya fundi", "Technician payments"},
                {"Matengenezo", "Maintenance"}, {"Mishahara", "Salaries"},
                {"Usafiri", "Transport"}, {"Mafuta", "Fuel"}, {"Manunuzi / stock", "Purchases / stock"},
                {"Bili za nyumbani", "Household bills"}, {"Afya", "Health"}, {"Elimu", "Education"},
                {"Kodi & vibali", "Taxes & permits"}, {"Bima", "Insurance"}, {"Mkopo", "Loan repayments"},
                {"Masoko & matangazo", "Marketing & advertising"}, {"Usafi", "Cleaning"},
                {"Ulinzi", "Security"}, {"Usafirishaji", "Delivery"}, {"Ada za benki", "Bank fees"},
                {"Vifaa vya kazi", "Work supplies"}, {"Benki", "Bank"},
                {"Bili inayojirudia", "Recurring bill"}, {"MAELEZO", "NOTES"},
                {"Weka jina, kiasi na tarehe sahihi.", "Enter a name, amount, and valid date."},
                {"Jumla ya miamala yote", "All transactions"}, {"Idadi ya miamala", "Transaction count"},
                {"Pakua ripoti ya CSV", "Export CSV report"}, {"Muhtasari kwa kundi", "Summary by category"},
                {"Hakuna miamala ya kuonyesha.", "No transactions to show."}, {"Pakua ripoti ya PDF", "Export PDF report"},
                {"Chapisha ripoti", "Print report"}, {"Ripoti ya fedha", "Financial report"},
                {"Miamala ya hivi karibuni", "Recent transactions"},
                {"Taarifa za biashara", "Business details"}, {"JINA LA BIASHARA *", "BUSINESS NAME *"},
                {"Sarafu na kikokotoo", "Currency and calculator"},
                {"Pata kiwango cha kubadilisha sarafu mtandaoni, au hesabu kiasi kwa kikokotoo.",
                        "Get an online exchange rate or calculate an amount."},
                {"Badili sarafu kwa rate ya sasa", "Convert currency at current rate"},
                {"Kiasi au hesabu", "Amount or calculation"}, {"Fungua kikokotoo", "Open calculator"},
                {"Kikokotoo", "Calculator"}, {"Tumia kiasi", "Use amount"},
                {"Hesabu haijakamilika au si sahihi.", "The calculation is incomplete or invalid."},
                {"Weka hesabu sahihi kwanza.", "Enter a valid calculation first."},
                {"Kibadilisha sarafu", "Currency converter"}, {"KIASI", "AMOUNT"},
                {"KUTOKA", "FROM"}, {"KWENDA", "TO"}, {"Rate ya sasa inahitaji intaneti.",
                        "Current exchange rates require internet."},
                {"Pata rate na badili", "Get rate and convert"},
                {"Weka kiasi sahihi.", "Enter a valid amount."},
                {"Inatafuta rate ya sasa...", "Fetching the latest exchange rate..."},
                {"Imeshindwa kupata rate. Angalia intaneti ujaribu tena.",
                        "Could not fetch the exchange rate. Check your internet and try again."},
                {"Updates za app", "App updates"},
                {"Toleo lililosakinishwa: ", "Installed version: "},
                {"Zima ukaguzi wa update kiotomatiki", "Turn off automatic update checks"},
                {"Washa ukaguzi wa update kiotomatiki", "Turn on automatic update checks"},
                {"Ukaguzi wa update utawashwa app inapofunguliwa.",
                        "Update checks will run when the app opens."},
                {"Ukaguzi wa update kiotomatiki umezimwa.", "Automatic update checks are off."},
                {"Angalia update sasa", "Check for updates now"},
                {"Ukaguzi wa kiotomatiki hufanyika app inapofunguliwa (kila baada ya saa 12). Android itaomba uthibitishe usakinishaji.",
                        "Automatic checks run when the app opens (every 12 hours). Android will ask you to confirm installation."},
                {"Update ya Mengidata", "Mengidata update"},
                {"Pakua na sakinisha", "Download and install"}, {"Baadaye", "Later"},
                {"Ruhusu kusakinisha update", "Allow update installation"},
                {"Washa ruhusa ya kusakinisha app kutoka chanzo hiki, kisha rudi Mipangilio na ujaribu tena.",
                        "Allow installs from this source, then return to Settings and try again."},
                {"Fungua ruhusa", "Open permission settings"},
                {"Imeshindwa kupakua update", "Update download failed"},
                {"Imeshindwa kuanzisha usakinishaji", "Could not start installation"},
                {"APK imepakuliwa. Thibitisha dirisha la Android kusakinisha.",
                        "The APK is downloaded. Confirm the Android dialog to install it."},
                {"Update imesakinishwa.", "Update installed."},
                {"Update haikusakinishwa", "Update was not installed"},
                {"Jaribu kusasisha tena baadaye.", "Try updating again later."},
                {"MMILIKI / MSIMAMIZI", "OWNER / MANAGER"}, {"MAELEZO MAFUPI", "TAGLINE"},
                {"ANWANI", "ADDRESS"}, {"SARAFU", "CURRENCY"}, {"TOVUTI", "WEBSITE"},
                {"SIMU", "PHONE"}, {"BARUA PEPE", "EMAIL"}, {"TIN", "TIN"},
                {"KIAMBISHI CHA NAMBA YA ANKARA", "INVOICE NUMBER PREFIX"},
                {"MAELEZO YA MALIPO", "PAYMENT DETAILS"}, {"UJUMBE KWENYE ANKARA", "INVOICE NOTE"},
                {"Hifadhi taarifa za biashara", "Save business details"}, {"Jina la biashara linahitajika.", "Business name is required."},
                {"Mipangilio imehifadhiwa.", "Settings saved."},
                {"Mipangilio ya ankara imehifadhiwa.", "Invoice settings saved."},
                {"Mipangilio ya ankara", "Invoice settings"}, {"KIAMBISHI CHA NAMBA YA ANKARA", "INVOICE NUMBER PREFIX"},
                {"Usalama na kufuli", "Security and app lock"},
                {"PIN imewashwa. App itajifunga ikifichwa.", "PIN is enabled. The app locks when hidden."},
                {"Weka PIN ili kuzuia watu wengine kufungua app.", "Set a PIN to prevent others from opening the app."},
                {"Badili PIN ya app", "Change app PIN"}, {"Weka PIN ya app", "Set app PIN"},
                {"Tumia tarakimu 6–12. Ukisahau PIN, hakuna njia ya kuirejesha bila kufuta data ya app.",
                        "Use 6–12 digits. If you forget the PIN, it cannot be recovered without deleting app data."},
                {"PIN ya sasa", "Current PIN"}, {"PIN mpya (tarakimu 6–12)", "New PIN (6–12 digits)"},
                {"Rudia PIN mpya", "Confirm new PIN"}, {"Badili PIN", "Change PIN"},
                {"Weka PIN", "Set PIN"}, {"Zima kufuli", "Turn off lock"},
                {"Zima kufuli la PIN?", "Turn off the PIN lock?"},
                {"Mtu yeyote anayeweza kufungua simu ataweza kufungua app hii.",
                        "Anyone who can unlock your phone will be able to open this app."},
                {"PIN imehifadhiwa.", "PIN saved."}, {"Kufuli la PIN limezimwa.", "PIN lock is off."},
                {"PIN iwe tarakimu 6–12, na isiwe mfululizo rahisi.",
                        "PIN must be 6–12 digits and not a simple sequence."},
                {"PIN mbili hazifanani.", "The PINs do not match."},
                {"Ingiza PIN yako ili kuendelea.", "Enter your PIN to continue."},
                {"Weka PIN kufungua app", "Enter PIN to unlock the app"},
                {"PIN yako", "Your PIN"}, {"Fungua app", "Unlock app"},
                {"PIN si sahihi. Jaribu tena.", "Incorrect PIN. Try again."},
                {"PIN si sahihi. Jaribu tena baada ya muda.", "Incorrect PIN. Try again later."},
                {"Subiri sekunde ", "Wait "}, {" kabla ya kujaribu tena.", " seconds before trying again."},
                {"Hitilafu ya usalama", "Security error"},
                {"Imeshindwa kuthibitisha PIN. Funga na ufungue app tena.",
                        "Could not verify the PIN. Close and reopen the app."},
                {"Imeshindwa kuhifadhi PIN kwa usalama. Jaribu tena.",
                        "Could not securely save the PIN. Try again."},
                {"PIN imefungwa kwa muda baada ya majaribio mengi. Subiri kisha ujaribu tena.",
                        "PIN is temporarily locked after too many attempts. Wait and try again."},
                {"PIN ya sasa si sahihi.", "Current PIN is incorrect."},
                {"Hifadhi mipangilio ya ankara", "Save invoice settings"},
                {"Data na nakala rudufu", "Data and backups"}, {"Pakua nakala rudufu (.json)", "Save backup (.json)"},
                {"Rejesha nakala rudufu (.json)", "Restore backup (.json)"}, {"Pakua miamala (.csv)", "Export transactions (.csv)"},
                {"Futa miamala, ankara na bili", "Delete transactions, invoices, and bills"},
                {"Futa data zote za fedha?", "Delete all financial data?"},
                {"Taarifa za biashara zitabaki. Data iliyofutwa haiwezi kurejeshwa bila nakala rudufu.", "Business details will remain. Deleted data cannot be recovered without a backup."},
                {"Futa data", "Delete data"}, {"Pakia data ya mfano", "Load demo data"},
                {"Pakia data ya mfano?", "Load demo data?"}, {"Pakia mfano", "Load demo"},
                {"Miamala, ankara na bili zilizopo zitabadilishwa na data ya mfano.", "Existing transactions, invoices, and bills will be replaced with demo data."},
                {"Hufanya kazi bila intaneti. Data yako huhifadhiwa kwenye simu hii.", "Works offline. Your data is stored on this phone."},
                {"Pakua nakala rudufu", "Save backup"}, {"Chagua faili kwenye simu", "Choose a file on your phone"},
                {"Washa vikumbusho vya bili", "Enable bill reminders"}, {"Zima vikumbusho vya bili", "Disable bill reminders"},
                {"Vikumbusho vya bili vimewashwa.", "Bill reminders are enabled."},
                {"Ruhusu arifa ili upokee vikumbusho vya bili.", "Allow notifications to receive bill reminders."},
                {"Hakuna bili zinazokuja.", "No upcoming bills."}, {"Faili limehifadhiwa.", "File saved."},
                {"Faili limeshifadhiwa. Fungua programu ya kushiriki.", "File saved. Opening share options."},
                {"Imeshindwa kuhifadhi faili.", "Could not save file."}, {"Imeshindwa kusoma faili", "Could not read file"},
                {"Faili si sahihi", "Invalid file"}, {"Chagua nakala rudufu sahihi ya Mengidata.", "Choose a valid Mengidata backup."},
                {"Rejesha nakala rudufu?", "Restore backup?"}, {"Data za sasa zitabadilishwa na data ya faili hili.", "Current data will be replaced with the data in this file."},
                {"Rejesha", "Restore"}, {"Data imerejeshwa.", "Data restored."},
                {"Chapishaji haupatikani", "Printing unavailable"}, {"Hifadhi ankara kama PDF badala yake.", "Save the invoice as a PDF instead."},
                {"Imeshindwa kutengeneza PDF", "Could not create PDF"}, {"Imeshindwa kutengeneza ripoti", "Could not create report"},
                {"Ankara ", "Invoice "}, {"ANKARA", "INVOICE"}, {"Mteja:", "Customer:"},
                {"Mwisho wa malipo", "Due date"}, {"Mwisho", "Due"}, {"Simu", "Phone"},
                {"Bei", "Price"}, {"Idadi", "Quantity"}, {"Hali", "Status"},
                {"Arifa ya bili hutumwa siku ya malipo saa 9:00 asubuhi. Bili isiyolipwa hukumbushwa kila siku.",
                        "Bill reminders are sent at 9:00 AM on the due date. Unpaid bills are reminded daily."},
                {"Zima vikumbusho vya bili", "Disable bill reminders"},
                {"Arifa za bili zinazofika siku ya malipo.", "Notifications for bills due."},
                {"Kikumbusho cha bili", "Bill reminder"}, {"Malipo yanatakiwa: ", "Payment due: "},
                {"Vikumbusho vya bili vimezimwa.", "Bill reminders are disabled."},
                {"Arifa hazijaruhusiwa", "Notifications are disabled"},
                {"Ruhusu arifa kwenye mipangilio ya simu ili upokee vikumbusho vya bili.",
                        "Allow notifications in your phone settings to receive bill reminders."},
                {"Vikumbusho vya bili vimewashwa.", "Bill reminders are enabled."},
                {"Miamala ya miezi 6", "6-month transactions"}, {"Mapato · ", "Income · "},
                {"Matumizi · ", "Expenses · "}, {"Bila kundi", "Uncategorized"},
                {"Fedha ndani", "Income"}, {"Fedha nje", "Expense"},
                {"Asante kwa kufanya biashara nasi.", "Thank you for your business."},
                {"Taarifa za Malipo", "Payment details"}, {"IMEWEKWA", "ISSUED"},
                {"Rasimu", "Draft"}, {"Imewekwa", "Issued"}, {"Malipo kwa kiasi", "Partially paid"},
                {"Malipo ya sehemu", "Partially paid"}, {"Haijalipwa", "Unpaid"},
                {"BILI KWA", "BILL TO"}, {"TAARIFA ZA ANKARA", "INVOICE DETAILS"},
                {"Namba", "Number"}, {"Jumla ndogo", "Subtotal"}, {"Punguzo", "Discount"},
                {"Maelezo", "Notes"}, {"Inaendelea", "Continued"},
                {"Malipo ya wateja", "Customer payments"}, {"Wamelipa", "Paid"},
                {"Wanaodaiwa", "Outstanding"}, {"Mteja amelipa?", "Customer paid?"},
                {"Hakuna ankara katika hali hii.", "No invoices in this status."},
                {"Weka haijalipwa?", "Mark as unpaid?"}, {"KUNDI MAALUM", "CUSTOM CATEGORY"},
                {"Andika kundi", "Enter a category"}, {"Chagua au andika kundi la muamala.", "Choose or enter a transaction category."},
                {"Huduma ya TV", "TV service"}, {"Vifurushi vya TV", "TV packages"},
                {"Ufungaji wa Dish", "Satellite dish installation"}, {"Mauzo ya vifaa", "Equipment sales"},
                {"Malipo ya huduma", "Service payment"}, {"Matumizi ya kawaida", "Regular expenses"},
                {"Matumizi ya nyumbani", "Household expenses"},
                {"Matumizi ya kila siku", "Daily expenses"}, {"Chakula", "Food"},
                {"Umeme", "Electricity"}, {"Maji", "Water"}, {"Kodi ya nyumba", "House rent"},
                {"Usafiri", "Transport"}, {"Afya", "Health"}, {"Elimu", "Education"},
                {"Mawasiliano", "Communications"}, {"Mafuta", "Fuel"}, {"Manunuzi / stock", "Purchases / stock"},
                {"Kodi & vibali", "Taxes & permits"}, {"Malipo ya wafanyakazi", "Staff payments"},
                {"Vifurushi vya TV", "TV packages"}, {"Mauzo ya vifaa", "Equipment sales"},
                {"Malipo ya huduma", "Service payment"}, {"Kiasi", "Amount"},
                {"Nyingine", "Other"}, {"Mauzo ya bidhaa", "Product sales"}
        };
        for (String[] pair : pairs) EN.put(pair[0], pair[1]);
    }

    private LocaleText() {}

    static boolean isEnglish(Context context) {
        english = "en".equals(preferences(context).getString(LANGUAGE, "sw"));
        return english;
    }

    static void toggle(Context context) {
        boolean next = !isEnglish(context);
        english = next;
        preferences(context).edit().putString(LANGUAGE, next ? "en" : "sw").apply();
    }

    static boolean isEnglish() { return english; }

    static String tr(Context context, String value) {
        if (value == null || !isEnglish(context)) return value;
        String translated = EN.get(value);
        if (translated != null) return translated;
        String prefix = null;
        for (Map.Entry<String, String> entry : EN.entrySet()) {
            String key = entry.getKey();
            if (value.length() > key.length() && value.startsWith(key)
                    && (Character.isWhitespace(key.charAt(key.length() - 1))
                    || value.charAt(key.length()) == ' ' || value.charAt(key.length()) == ':' || value.charAt(key.length()) == '·')
                    && (prefix == null || key.length() > prefix.length())) {
                prefix = key;
            }
        }
        return prefix == null ? value : EN.get(prefix) + value.substring(prefix.length());
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
