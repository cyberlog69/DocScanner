import SwiftUI

/// Document detail screen — mirrors Android DocumentDetailScreen.kt
struct DocumentDetailView: View {
    let document: MockDocument

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // TODO: Display scanned page images from shared FileService paths
                Image(systemName: "doc.text.image")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 300)
                    .cornerRadius(12)
                    .padding(.horizontal)

                Text("Extracted Text").font(.title2).bold().padding(.horizontal)
                // TODO: Show extracted text from shared KMP OcrResult
                Text("OCR text will appear here once the KMP shared framework is linked.")
                    .font(.body)
                    .padding(.horizontal)
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle(document.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}
