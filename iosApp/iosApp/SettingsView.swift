import SwiftUI
// import shared  // Uncomment after linking the KMP shared framework

/// Settings screen — mirrors Android SettingsDialog.kt
struct SettingsView: View {
    // TODO: Replace with shared KMP ScannerPreferences
    @State private var isBiometricEnabled = false
    @State private var selectedOcrLanguage = "English / Latin"

    let ocrLanguages = ["English / Latin", "Hindi / Devanagari", "Chinese", "Japanese", "Korean"]

    var body: some View {
        Form {
            Section("Security") {
                Toggle("Face ID / Touch ID Lock", isOn: $isBiometricEnabled)
            }
            Section("OCR Language") {
                Picker("Language", selection: $selectedOcrLanguage) {
                    ForEach(ocrLanguages, id: \.self) { lang in
                        Text(lang)
                    }
                }
            }
            Section("About") {
                LabeledContent("Version", value: "1.5.0-beta")
                LabeledContent("Privacy", value: "100% Offline — No internet")
            }
        }
        .navigationTitle("Settings")
    }
}
