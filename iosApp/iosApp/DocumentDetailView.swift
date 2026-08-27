import SwiftUI
import UIKit

/// Document detail screen — displays scanned pages, OCR text, and sharing actions
struct DocumentDetailView: View {
    let document: DocumentItem
    @EnvironmentObject var store: DocumentStore
    @Environment(\.dismiss) private var dismiss
    @State private var selectedPageIndex = 0
    @State private var isShowingShareSheet = false
    @State private var isShowingRenameDialog = false
    @State private var newTitle = ""
    @State private var isCopied = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Page Image Carousel / Viewer
                if !document.pageImagePaths.isEmpty {
                    TabView(selection: $selectedPageIndex) {
                        ForEach(0..<document.pageImagePaths.count, id: \.self) { index in
                            if let uiImage = UIImage(contentsOfFile: document.pageImagePaths[index]) {
                                Image(uiImage: uiImage)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(maxHeight: 380)
                                    .cornerRadius(12)
                                    .shadow(color: Color.black.opacity(0.1), radius: 6, x: 0, y: 3)
                                    .tag(index)
                            }
                        }
                    }
                    .frame(height: 400)
                    .tabViewStyle(PageTabViewStyle(indexDisplayMode: .automatic))

                    HStack {
                        Spacer()
                        Text("Page \(selectedPageIndex + 1) of \(document.pageImagePaths.count)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                }

                // Document Metadata Card
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text(document.categoryEmoji + " " + document.category)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.accentColor.opacity(0.12))
                            .foregroundColor(.accentColor)
                            .cornerRadius(8)

                        Spacer()

                        Button(action: { store.togglePin(document) }) {
                            Image(systemName: document.isPinned ? "pin.fill" : "pin")
                                .foregroundColor(document.isPinned ? .orange : .secondary)
                        }
                    }

                    Text(document.title)
                        .font(.title2)
                        .fontWeight(.bold)
                }
                .padding(.horizontal)

                Divider().padding(.horizontal)

                // Extracted OCR Text Section
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("Extracted Text")
                            .font(.headline)

                        Spacer()

                        Button(action: {
                            UIPasteboard.general.string = document.extractedText
                            isCopied = true
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                isCopied = false
                            }
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                                Text(isCopied ? "Copied" : "Copy")
                            }
                            .font(.caption)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Color(UIColor.secondarySystemFill))
                            .cornerRadius(6)
                        }
                    }

                    if document.extractedText.isEmpty {
                        Text("No text extracted for this document.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .padding(.vertical, 8)
                    } else {
                        Text(document.extractedText)
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
        .navigationTitle(document.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { isShowingShareSheet = true }) {
                    Image(systemName: "square.and.arrow.up")
                }
            }
        }
        .sheet(isPresented: $isShowingShareSheet) {
            if let firstPage = document.pageImagePaths.first,
               let image = UIImage(contentsOfFile: firstPage) {
                ActivityViewController(activityItems: [image, document.extractedText])
            }
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

