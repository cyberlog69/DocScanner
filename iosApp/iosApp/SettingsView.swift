import SwiftUI
// import shared  // Uncomment after linking the KMP shared framework

/// Settings screen — mirrors Android SettingsDialog.kt
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
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
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.6.0-beta").foregroundColor(.secondary)
                }
                HStack {
                    Text("Privacy")
                    Spacer()
                    Text("100% Offline — No internet").foregroundColor(.secondary)
                }
            }
        }
        .navigationTitle("Settings")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Done") {
                    dismiss()
                }
            }
        }
    }
}
