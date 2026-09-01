import SwiftUI
import UIKit

/// Document detail screen — displays scanned pages, OCR text, paper diagnostics, and sharing actions
struct DocumentDetailView: View {
    let document: DocumentItem
    @EnvironmentObject var store: DocumentStore
    @Environment(\.dismiss) private var dismiss
    @State private var selectedPageIndex = 0
    @State private var isShowingShareSheet = false
    @State private var isShowingRenameDialog = false
    @State private var newTitle = ""
    @State private var isCopied = false
    @State private var isRunningOcr = false
    @State private var liveExtractedText: String = ""

    var currentDocument: DocumentItem {
        store.documents.first(where: { $0.id == document.id }) ?? document
    }

    var effectiveText: String {
        liveExtractedText.isEmpty ? currentDocument.extractedText : liveExtractedText
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Page Image Carousel / Viewer
                if !currentDocument.pageImagePaths.isEmpty {
                    TabView(selection: $selectedPageIndex) {
                        ForEach(0..<currentDocument.pageImagePaths.count, id: \.self) { index in
                            if let uiImage = UIImage(contentsOfFile: currentDocument.pageImagePaths[index]) {
                                VStack {
                                    Image(uiImage: uiImage)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(maxHeight: 360)
                                        .cornerRadius(12)
                                        .shadow(color: Color.black.opacity(0.12), radius: 6, x: 0, y: 3)
                                }
                                .tag(index)
                            }
                        }
                    }
                    .frame(height: 380)
                    .tabViewStyle(PageTabViewStyle(indexDisplayMode: .automatic))

                    HStack {
                        Spacer()
                        Text("Page \(selectedPageIndex + 1) of \(currentDocument.pageImagePaths.count)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                }

                // Document Metadata Card
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text(currentDocument.categoryEmoji + " " + currentDocument.category)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.accentColor.opacity(0.12))
                            .foregroundColor(.accentColor)
                            .cornerRadius(8)

                        Spacer()

                        Button(action: { isShowingRenameDialog = true; newTitle = currentDocument.title }) {
                            Image(systemName: "pencil")
                                .foregroundColor(.secondary)
                        }

                        Button(action: { store.togglePin(currentDocument) }) {
                            Image(systemName: currentDocument.isPinned ? "pin.fill" : "pin")
                                .foregroundColor(currentDocument.isPinned ? .orange : .secondary)
                        }
                    }

                    Text(currentDocument.title)
                        .font(.title2)
                        .fontWeight(.bold)

                    // Page Dimension & Format Info
                    if selectedPageIndex < currentDocument.pageImagePaths.count,
                       let image = UIImage(contentsOfFile: currentDocument.pageImagePaths[selectedPageIndex]) {
                        let widthPx = Int(image.size.width * image.scale)
                        let heightPx = Int(image.size.height * image.scale)
                        let format = detectPaperFormat(width: widthPx, height: heightPx)

                        HStack(spacing: 12) {
                            Label("\(widthPx) × \(heightPx) px", systemImage: "aspectratio")
                            Label(format, systemImage: "doc.plaintext")
                        }
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal)

                Divider().padding(.horizontal)

                // Extracted OCR Text Section
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("Extracted OCR Text")
                            .font(.headline)

                        Spacer()

                        if isRunningOcr {
                            ProgressView()
                                .scaleEffect(0.8)
                        } else {
                            Button(action: { reRunOcr() }) {
                                Label("Re-scan", systemImage: "arrow.triangle.2.circlepath")
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color(UIColor.secondarySystemFill))
                                    .cornerRadius(6)
                            }
                        }

                        Button(action: {
                            UIPasteboard.general.string = effectiveText
                            isCopied = true
                            let generator = UINotificationFeedbackGenerator()
                            generator.notificationOccurred(.success)
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                isCopied = false
                            }
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                                Text(isCopied ? "Copied" : "Copy All")
                            }
                            .font(.caption)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Color.accentColor.opacity(0.15))
                            .foregroundColor(.accentColor)
                            .cornerRadius(6)
                        }
                    }

                    if effectiveText.isEmpty {
                        VStack(spacing: 8) {
                            Text("No OCR text extracted yet.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Button("Run Apple Neural Engine OCR") {
                                reRunOcr()
                            }
                            .font(.caption)
                            .buttonStyle(.borderedProminent)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                    } else {
                        Text(effectiveText)
                            .font(.system(.body, design: .monospaced))
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(10)
                    }
                }
                .padding(.horizontal)
            }
            .padding(.vertical)
        }
        .navigationTitle(currentDocument.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { isShowingShareSheet = true }) {
                    Image(systemName: "square.and.arrow.up")
                }
            }
        }
        .sheet(isPresented: $isShowingShareSheet) {
            if let firstPage = currentDocument.pageImagePaths.first,
               let image = UIImage(contentsOfFile: firstPage) {
                ActivityViewController(activityItems: [image, effectiveText])
            }
        }
        .alert("Rename Document", isPresented: $isShowingRenameDialog) {
            TextField("Document Title", text: $newTitle)
            Button("Cancel", role: .cancel) {}
            Button("Save") {
                store.renameDocument(currentDocument, newTitle: newTitle)
            }
        }
        .onAppear {
            if effectiveText.isEmpty && !currentDocument.pageImagePaths.isEmpty {
                reRunOcr()
            }
        }
    }

    private func reRunOcr() {
        var images = [UIImage]()
        for path in currentDocument.pageImagePaths {
            if let img = UIImage(contentsOfFile: path) {
                images.append(img)
            }
        }
        guard !images.isEmpty else { return }

        isRunningOcr = true
        OcrService.shared.recognizeText(from: images) { pages, combinedText in
            self.liveExtractedText = combinedText
            let autoCategory = OcrService.shared.classifyCategory(from: combinedText)
            self.store.updateDocumentOcr(docId: self.currentDocument.id, newText: combinedText, category: autoCategory)
            self.isRunningOcr = false
        }
    }

    private func detectPaperFormat(width: Int, height: Int) -> String {
        let maxDim = max(width, height)
        let minDim = min(width, height)
        guard minDim > 0 else { return "Custom" }
        let ratio = Double(maxDim) / Double(minDim)

        if ratio >= 1.39 && ratio <= 1.44 {
            return "A4 / ISO 216"
        } else if ratio >= 1.27 && ratio <= 1.32 {
            return "US Letter"
        } else if ratio >= 1.52 && ratio <= 1.62 {
            return "US Legal / ID Card"
        } else if ratio >= 2.0 {
            return "Receipt / Continuous"
        } else {
            return "Standard Document"
        }
    }
}

struct ActivityViewController: UIViewControllerRepresentable {
    var activityItems: [Any]
    var applicationActivities: [UIActivity]? = nil

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: applicationActivities)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
