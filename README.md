# Mengidata (v2.2.0)

Native offline-first Android business finance & accounting app built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Room**.

## Core Capabilities

- **Zero-Loss Auto-Accounting Engine**: Automatically classifies entries into **Pesa Kuingia (Money In)**, **Akiba (Savings)**, **Bili (Bills)**, and **Matumizi (Expenses)** with atomic Room transactions, ledger reconciliation, and automatic local safety snapshots so no money movement is lost.
- **High Bill Today Alert**: Automatically detects unusually high daily bills and highlights them in red in-app and in exported reports.
- **Color-Coded Excel & Separated CSV Exports (Zero-Permission SAF)**:
  - **Color-Coded Excel Report (`.xls`)**: 4 separated sheets (`Muhtasari_Auto`, `Miamala_Transactions`, `Bili_Bills`, `Akiba_Savings`) with visual cell coloring (Green = Money In, Red = High Bill Today / Overdue, Amber = Regular Bills, Orange = Expenses, Blue = Savings).
  - **Separated CSV Files (`.csv`)**: Dedicated individual exports for Transactions-only, Bills-only, Savings-only, and a Master Separated CSV with color hex codes, color meanings, and running balances.
- **Full Finance Suite**: Dashboard with 6-month Cashflow chart, Transactions (*Miamala*), Invoices (*Ankara*) with native A4 PDF export & print, Bills (*Bili*), and Savings Goals (*Akiba*).
- **Hardened Android 12+ App Update System**: Unified GitHub Releases checker (`mengikimaro-debug/mengi-data`), HTTPS redirect validation, live download progress, SHA-256 signing certificate verification against the installed package, and Android 12+ `PackageInstaller` (`PendingIntent.FLAG_MUTABLE`) with public `UpdateInstallReceiver`.
- **Bilingual & Adaptive UI**: Instant Swahili (`SW`) / English (`EN`) switching, Light / Dark Premium / System themes, and responsive `NavigationBar` / `NavigationRail` layout.
