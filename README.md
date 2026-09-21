<div align="center">

<img src="Logos/KiteLogo-Black-NOBG.png" alt="Kite Logo" width="120" />

# Kite

**Privacy-First, Local-Encrypted Personal Finance & Expense Tracker for Android**

[![Release](https://img.shields.io/github/v/release/MrFluffycloud/kite?style=flat-square&color=0F0F0F)](https://github.com/MrFluffycloud/kite/releases)
[![Android](https://img.shields.io/badge/Android-26%2B%20(API%2035)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.01-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Security](https://img.shields.io/badge/Encrypted-SQLCipher%20%2B%20Keystore-2E7D32?style=flat-square)](https://www.zetetic.net/sqlcipher/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)

*Kite gives you complete ownership over your financial life. Zero third-party trackers, zero mandatory cloud logins, 100% offline-first storage secured with hardware-backed encryption.*

</div>

---

## ✨ Highlights & Core Features

### 🔒 100% Privacy & Hardware-Backed Security
- **Local-Only by Design**: All transaction records, accounts, debts, and categories never leave your phone.
- **SQLCipher Database Encryption**: AES-256 encrypted database with cryptographic keys securely generated and stored in the **Android Keystore**.
- **Biometric App Lock**: Integrated biometric authentication (Fingerprint, Face Unlock, Device Credential) to keep your ledger protected.

### 💸 Debts & Intelligent Expense Splitting
- **Group & 1-on-1 Splits**: Split any expense among friends easily using **Equal**, **Custom Amount**, or **Percentage** methods.
- **Portion-Based Splitting**: Split either the entire transaction or just a **portion** of it (e.g. pay ₹500 at lunch but split only a ₹300 shared item).
- **Flexible Balance Settlements**: Settle outstanding balances in full or in **portions** (e.g. settle ₹50 out of ₹150, keeping the remaining ₹100 open).

### 🏷️ Dynamic Income & Expense Categorization
- **Context-Aware Category Sorting**: Categories dynamically adapt based on whether you are recording an **Expense** or an **Income** stream.
- **Pre-seeded & Customizable**: Default categories with multi-tier subcategories (e.g., Salary, Freelance, Groceries, Dining Out, Utilities) with full custom addition support.

### 📱 Modern, Fluid Jetpack Compose Interface
- **In-Place Transaction Expansion**: Smooth Samsung OneUI-inspired interaction where tapping any transaction smoothly expands its action drawer (**Edit**, **Duplicate**, **Details**, **Delete**) with unified ripple feedback.
- **Branded Startup Experience**: Fast startup with official Kite branding and seamless `#FAFAF9` window backgrounds to eliminate white flashes.
- **Interactive Analytics**: Monthly spending curve, category breakdowns, and financial health indicators.

### 💳 Multi-Account & Multi-Currency
- Support for Cash Wallets, Bank Accounts, Credit Cards, and Savings.
- Multi-currency transactions with base currency conversion.

---

## 🏗️ Architecture

Kite is built using modern Android **Clean Architecture** principles structured as a modular Gradle project:

```text
Kite/
├── app/                    # Application setup, navigation graph, DI wiring
├── core/
│   ├── model/              # Domain models (Transaction, Account, Person, Debt)
│   ├── domain/             # Business use cases (SplitExpenseUseCase, SettleDebtUseCase)
│   ├── data/               # Repository implementations and data mappers
│   └── database/           # Encrypted Room DB, SQLCipher, DAOs, Entity seeders
├── platform/
│   ├── security/           # Keystore key management, biometric auth, app lock
│   ├── notification/       # Local notifications, reminders
│   └── widget/             # Android Glance home screen widgets
└── feature/
    ├── home/               # Dashboard, summary cards, monthly analytics graph
    ├── transactions/       # Add/Edit transactions, transaction history, filters
    ├── debts/              # Debts dashboard, person details, split calculator
    ├── categories/         # Category management and icon pickers
    ├── accounts/           # Wallets, bank accounts, balance tracking
    ├── recurring/          # Subscriptions and scheduled expenses
    ├── insights/           # Deep spending analytics and reports
    ├── vault/              # Security settings, app lock configuration
    └── settings/           # App preferences, data backup & export
```

---

## 🛠️ Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Language**: Kotlin 2.1.0 with Coroutines & StateFlow
- **Dependency Injection**: [Koin](https://insert-koin.io/) 4.0.2
- **Database**: [Room 2.7.1](https://developer.android.com/training/data-storage/room) backed by [SQLCipher 4.6.1](https://github.com/sqlcipher/android-database-sqlcipher)
- **Security & Biometrics**: AndroidX Biometric 1.2.0 + Android KeyStore
- **CI/CD**: GitHub Actions automated release pipeline

---

## 🚀 Getting Started & Building

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer
- **JDK**: Java 17 or Java 21
- **Android SDK**: compileSdk 35, minSdk 26

### Clone & Run

```bash
# Clone the repository
git clone https://github.com/MrFluffycloud/kite.git
cd kite

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

The output APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📦 Releases & Downloads

Every tagged release automatically builds and attaches an installable `.apk` on GitHub Releases:

👉 **[Download the Latest Release APK](https://github.com/MrFluffycloud/kite/releases/latest)**

---

## 📄 License

```text
Copyright 2026 MrFluffycloud

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
