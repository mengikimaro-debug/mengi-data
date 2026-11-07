export type AppLanguage = 'sw' | 'en';

const englishTranslations: Record<string, string> = {
  'Dashibodi': 'Dashboard',
  'Miamala': 'Transactions',
  'Ankara': 'Invoices',
  'Bili': 'Bills',
  'Ripoti': 'Reports',
  'Mipangilio': 'Settings',
  'Dashibodi ya fedha': 'Finance dashboard',
  'Menyu': 'Menu',
  'Daftari la fedha': 'Financial ledger',
  'Ankara & malipo': 'Invoices & payments',
  'Malipo yanayokuja': 'Upcoming payments',
  'Uchambuzi': 'Analytics',
  'Sanidi mfumo': 'System setup',
  'Fungua menyu': 'Open menu',
  'Chagua lugha': 'Select language',
  'English': 'English',
  'Kiswahili': 'Swahili',
  'Ongeza': 'Add',
  'Ingiza mapato': 'Add income',
  'Mauzo au malipo ya mteja': 'Sales or customer payment',
  'Ingiza matumizi': 'Add expense',
  'Manunuzi au gharama za ofisi': 'Purchases or office expenses',
  'Tengeneza ankara': 'Create invoice',
  'Ankara ya PDF kwa mteja': 'PDF invoice for a customer',
  'Ongeza bili': 'Add bill',
  'Kodi, umeme, mishahara': 'Rent, utilities, salaries',
  'Taarifa za kumbusho': 'Reminder notifications',
  'Zote': 'All',
  'Zote ': 'All ',
  'Fedha ndani': 'Income',
  'Fedha nje': 'Expenses',
  'Salio la kipindi': 'Period balance',
  'Mwezi': 'Month',
  'Robo': 'Quarter',
  'Mwaka': 'Year',
  'Yote': 'All time',
  'Zinazodaiwa': 'Outstanding',
  'Rasimu': 'Drafts',
  'Hazijalipwa': 'Unpaid',
  'Zimelipwa': 'Paid',
  'Tafuta bili...': 'Search bills...',
  'Tafuta ankara...': 'Search invoices...',
  'Tafuta miamala...': 'Search transactions...',
  'Taarifa za biashara': 'Business profile',
  'Hizi zinaonekana kwenye ankara zako na nyaraka za PDF.': 'These appear on your invoices and PDF documents.',
  'Jina la biashara *': 'Business name *',
  'Jina la mmiliki / Msimamizi': 'Owner / Administrator name',
  'Maelezo mafupi ya biashara (Tagline)': 'Business tagline',
  'Anwani ya ofisi': 'Office address',
  'Namba ya simu': 'Phone number',
  'Barua pepe': 'Email',
  'Tovuti au ukurasa wa mtandao': 'Website or social page',
  'Hifadhi taarifa za biashara': 'Save business profile',
  'Ankara & maelezo ya malipo': 'Invoice & payment details',
  'Hifadhi mipangilio ya ankara': 'Save invoice settings',
  'Data & nakala rudufu': 'Data & backups',
  'Pakua nakala rudufu': 'Download backup',
  'Leta data kutoka faili': 'Import data from file',
  'Pakua miamala (CSV)': 'Download transactions (CSV)',
  'Hali ya data': 'Data status',
  'Pakia data ya mfano': 'Load sample data',
  'Inafanya kazi bila intaneti': 'Works offline',
  'Eneo la hatari': 'Danger zone',
  'Muonekano wa mfumo': 'Appearance',
  'Chagua mandhari unayopendelea.': 'Choose your preferred theme.',
  'Mfumo': 'System',
  'Mwanga': 'Light',
  'Giza Premium': 'Premium Dark',
  'Fuata mandhari ya simu': 'Follow your phone theme',
  'Muonekano angavu': 'Bright appearance',
  'Kijani cha kina na lime': 'Deep green with lime accents',
  'Updates za programu': 'App updates',
  'Toleo lililosakinishwa': 'Installed version',
  'Tovuti husasishwa kiotomatiki unapofungua toleo jipya.': 'The website updates automatically when a new version is published.',
  'Salama': 'Verified',
  'Angalia updates kiotomatiki': 'Check for updates automatically',
  'Huangalia hadi mara moja kila baada ya saa 12 unapofungua app.': 'Checks at most once every 12 hours when you open the app.',
  'Angalia update sasa': 'Check for updates',
  'Pakua na sakinisha': 'Download and install',
  'Ruhusu kusakinisha update': 'Allow update installation',
  'Toleo jipya linapatikana': 'A new version is available',
  'Data yako itabaki baada ya kusasisha.': 'Your data will remain after updating.',
  'Inapakua update salama...': 'Downloading verified update...',
  'Pakua imekamilika. Thibitisha dirisha la Android.': 'Download complete. Confirm the Android installer.',
  'Tumia toleo la hivi karibuni la tovuti.': 'You are using the latest website version.',
  'Pakua app ya Android': 'Download Android app',
  'Imeshindwa kuanzisha ukaguzi wa update.': 'Could not start the update check.',
  'Imeshindwa kuhifadhi chaguo la update.': 'Could not save the update preference.',
  'Imeshindwa kuanzisha upakuaji wa update.': 'Could not start the update download.',
  'Chagua kipindi': 'Select period',
  'Chuja miamala': 'Filter transactions',
  'Chuja ankara': 'Filter invoices',
  'Chuja bili': 'Filter bills',
  'Hifadhi': 'Save',
  'Ongeza miamala': 'Add transaction',
  'Jina la biashara linahitajika.': 'Business name is required.',
  'Imelipwa': 'Paid',
  'Haijalipwa': 'Unpaid',
  'Malipo ya sehemu': 'Partially paid',
  'Maelezo': 'Description',
  'Kiasi': 'Amount',
  'Tarehe': 'Date',
  'Aina ya malipo': 'Payment method',
  'Aina': 'Type',
  'Kategoria': 'Category',
  'Mteja': 'Customer',
  'Mtoa huduma': 'Payee',
  'Hifadhi miamala': 'Save transaction',
  'Hifadhi bili': 'Save bill',
  'Hifadhi ankara': 'Save invoice',
  'Funga': 'Close',
  'Ghairi': 'Cancel',
  'Hariri': 'Edit',
  'Futa': 'Delete',
  'Tazama': 'View',
  'Pakua PDF': 'Download PDF',
  'Weka imelipwa': 'Mark as paid',
  'Mapato Yote': 'Total income',
  'Matumizi Yote': 'Total expenses',
  'Faida Halisi': 'Net profit',
  'Mapato': 'Income',
  'Matumizi': 'Expenses',
  'Faida': 'Profit',
  'Mwezi huu': 'This month',
  'Wiki hii': 'This week',
  'Leo': 'Today',
  'Tafuta...': 'Search...',
  'Hakuna matokeo.': 'No results.',
  'Hakuna data.': 'No data.',
  'Asante kwa kufanya biashara nasi.': 'Thank you for doing business with us.',
  'Mauzo ya bidhaa': 'Product sales',
  'Huduma': 'Services',
  'Ushauri': 'Consulting',
  'Mafunzo': 'Training',
  'Uwekezaji': 'Investment',
  'Nyingine': 'Other',
  'Matumizi ya kila siku': 'Daily expenses',
  'Bili za nyumbani': 'Household bills',
  'Manunuzi / stock': 'Purchases / stock',
  'Kodi ya pango': 'Rent',
  'Umeme': 'Electricity',
  'Maji': 'Water',
  'Umeme & maji': 'Utilities',
  'Internet & simu': 'Internet & phone',
  'Chakula': 'Food',
  'Malipo ya fundi': 'Technician payments',
  'Mishahara': 'Salaries',
  'Usafiri': 'Transport',
  'Mafuta': 'Fuel',
  'Kodi & vibali': 'Taxes & permits',
  'Matengenezo': 'Maintenance',
  'Masoko & matangazo': 'Marketing & advertising',
  'Afya': 'Health',
  'Elimu': 'Education',
  'Mkopo': 'Loan repayments',
  'Bima': 'Insurance',
  'Usafi': 'Cleaning',
  'Ulinzi': 'Security',
  'Usafirishaji': 'Delivery',
  'Ada za benki': 'Bank fees',
  'Vifaa vya kazi': 'Work supplies',
  'Januari': 'January',
  'Februari': 'February',
  'Machi': 'March',
  'Aprili': 'April',
  'Mei': 'May',
  'Juni': 'June',
  'Julai': 'July',
  'Agosti': 'August',
  'Septemba': 'September',
  'Oktoba': 'October',
  'Novemba': 'November',
  'Desemba': 'December'
};

const additionalTranslations: Record<string, string> = {
  'kutoka mwezi uliopita': 'from last month'
  ,'Mmiliki': 'Owner'
  ,'Salio la mwezi': 'Monthly balance'
  ,'Kila shilingi inayoingia na kutoka — kwenye rekodi moja safi na rahisi kufuatilia.': 'Every shilling in and out, in one clear and easy-to-follow record.'
  ,'Muhtasari wa biashara': 'Business overview'
  ,'Habari, ': 'Hello, '
  ,'Hapa ndipo pesa zako zinapokutana. Tazama mapato, matumizi na ankara kwa mtazamo mmoja.': 'See your income, expenses and invoices together in one place.'
  ,'Data ya mfano.': 'Sample data.'
  ,'Tazama jinsi Mengidata inavyofanya kazi, kisha futa na uanze data yako halisi.': 'Explore Mengidata, then clear the sample and start entering your own records.'
  ,'Anza data yangu': 'Start with my data'
  ,'Pata maelezo zaidi': 'Learn more'
  ,'Fedha zilizokusanywa': 'Income collected'
  ,'Jumla ya matumizi': 'Total expenses'
  ,'Faida halisi': 'Net profit'
  ,'Ankara mpya': 'New invoice'
  ,'Jumla iliyotolewa': 'Total issued'
  ,'Iliyolipwa': 'Paid'
  ,'Inayodaiwa': 'Due'
  ,'Tengeneza ankara za kitaalamu, pakua PDF, na fuatilia kila malipo kwa urahisi.': 'Create professional invoices, download PDFs and easily track every payment.'
  ,'Tafuta mteja au namba...': 'Search customer or number...'
  ,'Kumbuka kila bili na tarehe yake ya malipo — ili biashara isikwame kwa kusahau.': 'Keep track of every bill and due date so your business stays on schedule.'
  ,'Zimechelewa': 'Overdue'
  ,'Zilizolipwa mwezi huu': 'Paid this month'
  ,'Hakuna bili zinazolingana': 'No matching bills'
  ,'Ongeza bili mpya': 'Add a new bill'
  ,'Bili mpya ya pango, umeme, mishahara au kodi ya biashara.': 'Add a new rent, utility, salary or business tax bill.'
  ,'Picha kamili ya utendaji wa biashara yako — mwezi kwa mwezi, kundi kwa kundi.': 'A complete view of your business performance, month by month and category by category.'
  ,'Uchambuzi wa biashara': 'Business analytics'
  ,'Toa CSV': 'Export CSV'
  ,'Chapisha ripoti': 'Print report'
  ,'Sanidi taarifa za biashara yako, mfumo wa ankara na usimamizi wa data na nakala rudufu.': 'Set up your business details, invoicing, data and backups.'
  ,'Mfano: Mengidata': 'Example: Mengidata'
  ,'Mfano: Mengi': 'Example: Mengi'
  ,'Mfano: Ushauri, mauzo & huduma za kidijitali': 'Example: Consulting, sales & digital services'
  ,'Mfano: Kimara Mwisho, Ubungo, Dar es Salaam': 'Example: Kimara Mwisho, Ubungo, Dar es Salaam'
  ,'TIN / Namba ya Kodi ya Mlipaji': 'TIN / Taxpayer number'
  ,'Hiari': 'Optional'
  ,'Mipangilio hii itatumika moja kwa moja unapotengeneza ankara mpya.': 'These settings will be applied to new invoices.'
  ,'Mwanzo wa namba ya ankara (Prefix)': 'Invoice number prefix'
  ,'Mfano:': 'Example:'
  ,'VAT ya kawaida (%)': 'Standard VAT (%)'
  ,'Weka 0 ikiwa haujasajiliwa kulipa VAT.': 'Enter 0 if you are not registered for VAT.'
  ,'Maelezo ya malipo (M-Pesa, Benki, n.k.)': 'Payment details (M-Pesa, bank, etc.)'
  ,'Maelezo ya chini ya ankara': 'Invoice footer note'
  ,'Pakua Backup (.json)': 'Download backup (.json)'
  ,'Chagua faili la JSON': 'Choose JSON file'
  ,'Pakua CSV ya Miamala': 'Download transactions CSV'
  ,'Faili la JSON lenye miamala, ankara, bili na mipangilio yote.': 'A JSON file containing all transactions, invoices, bills and settings.'
  ,'Data yako inahifadhiwa kwenye kifaa hiki. Pakua nakala rudufu mara kwa mara.': 'Your data is stored on this device. Download backups regularly.'
  ,'Inayofanya kazi bila intaneti': 'Works offline'
  ,'Kitendo hiki kitafuta miamala, ankara na bili zote kwenye hifadhi ya ndani ya kifaa hiki. Hakikisha umepakua backup kwanza.': 'This will permanently remove all transactions, invoices and bills from this device. Download a backup first.'
  ,'Rejesha nakala ya awali iliyopakuliwa kutoka Mengidata.': 'Restore the backup previously downloaded from Mengidata.'
  ,'Kiasi (TZS) *': 'Amount (TZS) *'
  ,'Tarehe *': 'Date *'
  ,'Maelezo ya miamala *': 'Transaction description *'
  ,'Fedha ndani (Mapato)': 'Money in (Income)'
  ,'Fedha nje (Matumizi)': 'Money out (Expenses)'
  ,'Mfano: Mauzo ya vifaa vya ofisi au Malipo ya umeme': 'Example: Office equipment sale or electricity payment'
  ,'Jina la mteja': 'Customer name'
  ,'Aina ya miamala': 'Transaction type'
  ,'Chagua': 'Select'
  ,'Tafadhali weka kiasi kikubwa kuliko sifuri.': 'Please enter an amount greater than zero.'
  ,'Tafadhali weka maelezo ya miamala.': 'Please enter a transaction description.'
  ,'Hariri miamala': 'Edit transaction'
  ,'Hakuna ankara au bili iliyochelewa. Kila kitu kiko salama!': 'No overdue invoices or bills. Everything is up to date!'
  ,'Imechelewa': 'Overdue'
};

const transactionTranslations: Record<string, string> = {
  'Kila shilingi inayoingia na kutoka — kwenye rekodi moja safi na rahisi kufuatilia.': 'Every shilling in and out, in one clear and easy-to-follow record.'
  ,'Tafuta maelezo, mteja...': 'Search description, customer...'
  ,'Miezi yote': 'All months'
  ,'Hakuna miamala': 'No transactions'
  ,'Hakuna rekodi inayolingana na vichujio ulivyochagua. Jaribu kubadilisha vigezo vya utafutaji.': 'No records match the selected filters. Try changing your search.'
  ,'Bado hujaongeza miamala. Anza kwa kurekodi mauzo au matumizi ya kwanza.': 'You have not added transactions yet. Start by recording your first sale or expense.'
  ,'Tarehe': 'Date'
  ,'Maelezo & Mhusika': 'Description & party'
  ,'Kundi': 'Category'
  ,'Njia': 'Method'
  ,'Kiasi (TZS)': 'Amount (TZS)'
  ,'Vitendo': 'Actions'
  ,'Chanzo': 'Source'
};

const featureTranslations: Record<string, string> = {
  'Kila shilingi inayoingia na kutoka — kwenye rekodi moja safi na rahisi kufuatilia.': 'Every shilling in and out, in one clear and easy-to-follow record.'
  ,'Kiasi (TZS) *': 'Amount (TZS) *'
  ,'Tarehe *': 'Date *'
  ,'Maelezo ya miamala *': 'Transaction description *'
  ,'Mteja / Mlipaji': 'Customer / Payer'
  ,'Msambazaji / Mlipwaji': 'Supplier / Payee'
  ,'Jina la mtu au kampuni': 'Person or company name'
  ,'Kundi': 'Category'
  ,'Njia ya malipo': 'Payment method'
  ,'Namba ya kumbukumbu (Ref)': 'Reference number'
  ,'Mfano: MP-9941 au CRDB-201': 'Example: MP-9941 or CRDB-201'
  ,'Maelezo ya ziada (Hiari)': 'Additional notes (Optional)'
  ,'Maelezo yoyote mengine kuhusu miamala hii...': 'Any other details about this transaction...'
  ,'Miamala hii itaonekana kwenye dashibodi mara moja.': 'This transaction will appear on your dashboard immediately.'
  ,'Tafadhali weka jina la bili.': 'Please enter a bill name.'
  ,'Tafadhali weka kiasi cha bili.': 'Please enter the bill amount.'
  ,'Hariri bili': 'Edit bill'
  ,'Jina la bili *': 'Bill name *'
  ,'Mfano: Kodi ya pango — Novemba au LUKU ya Ofisi': 'Example: November rent or office electricity'
  ,'Tarehe ya mwisho ya malipo *': 'Payment due date *'
  ,'Kundi la bili': 'Bill category'
  ,'Unamlipa nani? (Payee)': 'Who are you paying?'
  ,'Jina la mwenye nyumba, TANESCO, n.k.': 'Landlord, utility provider, etc.'
  ,'Njia ya kulipa': 'Payment method'
  ,'Ankara mpya': 'New invoice'
  ,'Jumla iliyotolewa': 'Total issued'
  ,'Iliyolipwa': 'Paid'
  ,'Inayodaiwa': 'Due'
  ,'Tengeneza ankara za kitaalamu, pakua PDF, na fuatilia kila malipo kwa urahisi.': 'Create professional invoices, download PDFs and easily track every payment.'
  ,'Tafuta mteja au namba...': 'Search customer or number...'
  ,'Imepakuliwa kama PDF.': 'downloaded as PDF.'
  ,'PDF haikuweza kuundwa. Tafadhali jaribu tena.': 'The PDF could not be created. Please try again.'
  ,'Muhtasari wa biashara': 'Business overview'
  ,'Habari, ': 'Hello, '
  ,'Hapa ndipo pesa zako zinapokutana. Tazama mapato, matumizi na ankara kwa mtazamo mmoja.': 'See your income, expenses and invoices together in one place.'
  ,'Data ya mfano.': 'Sample data.'
  ,'Tazama jinsi Mengidata inavyofanya kazi, kisha futa na uanze data yako halisi.': 'Explore Mengidata, then clear the sample and start entering your own records.'
  ,'Anza data yangu': 'Start with my data'
  ,'Faida halisi': 'Net profit'
  ,'Mtiririko wa fedha': 'Cash flow'
  ,'Miezi 6 iliyopita · mapato na matumizi': 'Last 6 months · income and expenses'
  ,'Bili zinazokaribia': 'Upcoming bills'
  ,'Malipo yanayohitaji uangalifu': 'Payments that need attention'
  ,'Hakuna bili inayodaiwa kwa sasa.': 'No bills are currently due.'
  ,'Jumla inayodaiwa': 'Total due'
  ,'Endesha bili': 'Manage bills'
  ,'Miamala ya karibuni': 'Recent transactions'
  ,'Fedha zilizoingia na kutoka hivi karibuni': 'Recent money in and out'
  ,'Bado hakuna miamala iliyorekodiwa.': 'No transactions have been recorded yet.'
  ,'Matumizi kwa kundi': 'Expenses by category'
  ,'Hakuna matumizi kwa kipindi hiki.': 'No expenses for this period.'
  ,'Uchambuzi wa biashara': 'Business analytics'
  ,'Picha kamili ya utendaji wa biashara yako — mwezi kwa mwezi, kundi kwa kundi.': 'A complete view of your business performance, month by month and category by category.'
  ,'Ripoti ya mwaka': 'Annual report'
  ,'Mapato yote': 'Total income'
  ,'Matumizi yote': 'Total expenses'
  ,'Margin ya faida': 'Profit margin'
  ,'Ankara zilizotolewa': 'Invoices issued'
  ,'Ankara zinazodaiwa': 'Outstanding invoices'
  ,'Mapato, matumizi na faida kwa miezi': 'Income, expenses and profit by month'
  ,'miezi ya hivi karibuni yenye rekodi': 'most recent months with records'
  ,'Jumla': 'Total'
  ,'Pakua CSV': 'Download CSV'
  ,'Chapisha ripoti': 'Print report'
  ,'Miezi yote': 'All months'
  ,'Kiswahili': 'Swahili'
};

const uiTranslations: Record<string, string> = {
  'Kiswahili': 'Swahili',
  'yote': 'all',
  'kutoka ': 'from ',
  'margin ya faida': 'profit margin',
  'hazijalipwa': 'unpaid',
  'zimechelewa': 'overdue',
  'Kiwango cha juu:': 'Maximum:',
  'Ankara zinazodaiwa': 'Outstanding invoices',
  'Bili zinazokaribia': 'Upcoming bills',
  'Miamala ya karibuni': 'Recent transactions',
  'Matumizi kwa kundi': 'Expenses by category',
  'Namba hii ya ankara tayari inatumika. Tumia namba tofauti.': 'This invoice number is already in use. Choose another number.',
  'Tafadhali weka jina la mteja.': 'Please enter the customer name.',
  'Tafadhali weka namba ya ankara.': 'Please enter the invoice number.',
  'Tafadhali ongeza angalau bidhaa au huduma moja yenye maelezo.': 'Please add at least one described product or service.',
  'Fungua ankara husika': 'Open related invoice',
  'Fungua bili husika': 'Open related bill',
  'Futa miamala': 'Delete transaction',
  'Ondoa vichujio vyote': 'Clear all filters',
  'Hakuna miamala inayolingana': 'No matching transactions',
  'Taarifa za biashara': 'Business profile',
  'Mipangilio imehifadhiwa.': 'Settings saved.',
  'Imesasishwa': 'Updated',
  'miamala': 'transactions',
  'ankara': 'invoices',
  'bili': 'bills',
  'Chagua mandhari': 'Choose a theme',
  'Mengidata / Dashibodi': 'Mengidata / Dashboard',
  'Tafadhali weka jina la bili.': 'Please enter a bill name.',
  'Tafadhali weka kiasi cha bili.': 'Please enter the bill amount.',
  'Kila bili na tarehe yake ya malipo — ili biashara isikwame kwa kusahau.': 'Track each bill and due date so your business stays on schedule.',
  'Malipo yanayohitaji uangalifu': 'Payments that need attention',
  'Hakuna bili inayodaiwa kwa sasa.': 'No bills are currently due.',
  'Miezi 6 iliyopita · mapato na matumizi': 'Last 6 months · income and expenses',
  'Imechaguliwa': 'Selected',
  'Rejesha data kutoka kwenye faili?': 'Restore data from file?',
  'Data zote zilizopo sasa zitabadilishwa na data iliyomo kwenye faili hili. Hakikisha umehifadhi nakala rudufu ya sasa kwanza.': 'All existing data will be replaced by the data in this file. Make sure you have a backup first.',
  'Leta data': 'Import data',
  'Futa data zote za kifedha?': 'Delete all financial data?',
  'Miamala yote, ankara zote na bili zote zitaondolewa kabisa. Taarifa za biashara yako zitabaki. Kitendo hiki hakiwezi kutenduliwa.': 'All transactions, invoices and bills will be permanently removed. Your business details will remain. This action cannot be undone.',
  'Futa data zote': 'Delete all data',
  'Pakia data ya mfano?': 'Load sample data?',
  'Mfumo utapakiwa miamala, ankara na bili za mfano ili kuona jinsi Mengidata inavyofanya kazi.': 'Sample transactions, invoices and bills will be loaded so you can explore Mengidata.',
  'Mfano wa': 'Example of',
  'Jina la biashara linahitajika.': 'Business name is required.',
  'Tafadhali weka kiasi kikubwa kuliko sifuri.': 'Please enter an amount greater than zero.',
  'Tafadhali weka maelezo ya miamala.': 'Please enter a transaction description.',
  'PDF haikuweza kuundwa. Tafadhali jaribu tena.': 'The PDF could not be created. Please try again.',
  'Hakuna ankara au bili iliyochelewa. Kila kitu kiko salama!': 'No overdue invoices or bills. Everything is up to date!',
  'malipo yanahitaji uangalifu': 'payments need attention',
  'ankara na': 'invoices and',
  'bili zimechelewa malipo.': 'bills are overdue.',
  'Imetolewa:': 'Generated:',
  'Maelezo ya ziada (Hiari)': 'Additional notes (Optional)',
  'Hifadhi miamala': 'Save transaction',
  'Hifadhi bili': 'Save bill'
  ,'Hariri ankara': 'Edit invoice'
  ,'Jina la mteja *': 'Customer name *'
  ,'Nani unamtolea ankara?': 'Who is this invoice for?'
  ,'Simu': 'Phone'
  ,'Anwani / Eneo': 'Address / Location'
  ,'Mtaa, Mji, n.k.': 'Street, city, etc.'
  ,'Taarifa za ankara': 'Invoice details'
  ,'Namba, hali na tarehe': 'Number, status and dates'
  ,'Namba ya ankara *': 'Invoice number *'
  ,'Hali ya ankara': 'Invoice status'
  ,'Imetolewa (Rasmi)': 'Issued'
  ,'Rasimu (Inatayarishwa)': 'Draft'
  ,'Tarehe ya kutolea': 'Issue date'
  ,'Tarehe ya mwisho wa malipo': 'Due date'
  ,'Ongeza vitu unavyotoza': 'Add items you charge for'
  ,'Ongeza mstari': 'Add line'
  ,'Idadi': 'Quantity'
  ,'Bei (TZS)': 'Price (TZS)'
  ,'Jumla ya bidhaa (idadi × bei)': 'Items total (quantity × price)'
  ,'Weka 0 kama haujasajiliwa na VAT.': 'Enter 0 if you are not registered for VAT.'
  ,'Weka kiasi chote': 'Pay in full'
  ,'Maelezo yataonekana chini ya ankara...': 'Notes will appear at the bottom of the invoice...'
  ,'Jumla ya ankara': 'Invoice total'
  ,'Jumla ndogo': 'Subtotal'
  ,'Jumla kuu': 'Grand total'
  ,'Kiasi kilicholipwa': 'Amount paid'
  ,'Salio linalodaiwa': 'Balance due'
  ,'Hali ya malipo:': 'Payment status:'
  ,'Kidokezo:': 'Tip:'
  ,'Ankara iliyolipwa itaingiza mapato kwenye daftari la miamala kiotomatiki. PDF inazalishwa bila hitaji la internet.': 'Paid invoices automatically add income to the transaction ledger. PDFs are generated offline.'
  ,'Namba:': 'Number:'
  ,'Hifadhi rasimu': 'Save draft'
  ,'Hifadhi & Pakua PDF': 'Save & download PDF'
  ,'Tarehe iliyolipwa (kama tayari)': 'Payment date (if already paid)'
  ,'Bili hii imelipwa tayari': 'This bill has already been paid'
  ,'Namba ya mita, akaunti ya benki, masharti ya bili...': 'Meter number, bank account or bill terms...'
};

export function translate(language: AppLanguage, text: string): string {
  if (language === 'sw') return text;
  return englishTranslations[text] ||
    additionalTranslations[text] ||
    transactionTranslations[text] ||
    featureTranslations[text] ||
    uiTranslations[text] ||
    text;
}