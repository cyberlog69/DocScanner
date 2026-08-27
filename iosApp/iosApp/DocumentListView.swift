import SwiftUI
import UIKit
import VisionKit

struct DocumentItem: Identifiable, Codable {
    var id: String
    var title: String
    var category: String
    var createdAt: Double
    var pageCount: Int
    var thumbnailPath: String
    var pdfPath: String
    var extractedText: String
    var isPinned: Bool
    var tags: [String]
    var pageImagePaths: [String]

    var categoryEmoji: String {
        switch category.uppercased() {
        case "RECEIPT", "RECEIPTS": return "🧾"
        case "ID_CARD", "ID CARDS": return "🪪"
        case "NOTE", "NOTES": return "📝"
        case "CONTRACT", "CONTRACTS": return "📋"
        case "INVOICE", "INVOICES": return "💼"
        case "BOOK", "BOOKS": return "📚"
        default: return "📎"
        }
    }
}

class DocumentStore: ObservableObject {
    @Published var documents: [DocumentItem] = []

    private let saveFileName = "docscanner_items.json"

    init() {
        loadDocuments()
    }

    private var fileURL: URL {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        return paths[0].appendingPathComponent(saveFileName)
    }

    func loadDocuments() {
        guard let data = try? Data(contentsOf: fileURL),
              let items = try? JSONDecoder().decode([DocumentItem].self, from: data) else {
            self.documents = []
            return
        }
        self.documents = items
    }

    func saveDocuments() {
        if let data = try? JSONEncoder().encode(documents) {
            try? data.write(to: fileURL)
        }
    }

    func addDocument(title: String, category: String, pageImages: [UIImage], extractedText: String) {
        let docId = UUID().uuidString
        let fileManager = FileManager.default
        let docDir = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(docId)
        try? fileManager.createDirectory(at: docDir, withIntermediateDirectories: true)

        var pagePaths: [String] = []
        for (index, image) in pageImages.enumerated() {
            let pageFile = docDir.appendingPathComponent("page_\(index).jpg")
            if let jpegData = image.jpegData(compressionQuality: 0.92) {
                try? jpegData.write(to: pageFile)
                pagePaths.append(pageFile.path)
            }
        }

        let thumbPath = pagePaths.first ?? ""
        let pdfPath = docDir.appendingPathComponent("document.pdf").path

        let item = DocumentItem(
            id: docId,
            title: title.isEmpty ? "Document \(Int(Date().timeIntervalSince1970))" : title,
            category: category,
            createdAt: Date().timeIntervalSince1970,
            pageCount: pagePaths.count,
            thumbnailPath: thumbPath,
            pdfPath: pdfPath,
            extractedText: extractedText,
            isPinned: false,
            tags: [],
            pageImagePaths: pagePaths
        )

        DispatchQueue.main.async {
            self.documents.insert(item, at: 0)
            self.saveDocuments()
        }
    }

    func deleteDocument(_ doc: DocumentItem) {
        let fileManager = FileManager.default
        let docDir = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(doc.id)
        try? fileManager.removeItem(at: docDir)

        DispatchQueue.main.async {
            self.documents.removeAll { $0.id == doc.id }
            self.saveDocuments()
        }
    }

    func togglePin(_ doc: DocumentItem) {
        if let index = documents.firstIndex(where: { $0.id == doc.id }) {
            documents[index].isPinned.toggle()
            saveDocuments()
        }
    }

    func renameDocument(_ doc: DocumentItem, newTitle: String) {
        if let index = documents.firstIndex(where: { $0.id == doc.id }) {
            documents[index].title = newTitle.trimmingCharacters(in: .whitespacesAndNewlines)
            saveDocuments()
        }
    }
}

/// Document list screen — mirrors Android DocumentListScreen.kt
struct DocumentListView: View {
    @EnvironmentObject var store: DocumentStore
    @State private var searchQuery = ""
    @State private var selectedCategory = "All"
    @State private var isShowingScanner = false
    @State private var isShowingSettings = false
    @State private var isGridView = true

    let categories = ["All", "Receipts", "ID Cards", "Notes", "Contracts", "Invoices", "Books", "Other"]

    var filteredDocuments: [DocumentItem] {
        store.documents.filter { doc in
            let matchesCategory = selectedCategory == "All" || doc.category.localizedCaseInsensitiveContains(selectedCategory)
            let matchesQuery = searchQuery.isEmpty || doc.title.localizedCaseInsensitiveContains(searchQuery) || doc.extractedText.localizedCaseInsensitiveContains(searchQuery)
            return matchesCategory && matchesQuery
        }.sorted { (d1, d2) in
            if d1.isPinned != d2.isPinned {
                return d1.isPinned && !d2.isPinned
            }
            return d1.createdAt > d2.createdAt
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Category Chips
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(categories, id: \.self) { cat in
                        Button(action: { selectedCategory = cat }) {
                            Text(cat)
                                .font(.subheadline)
                                .fontWeight(selectedCategory == cat ? .semibold : .regular)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 7)
                                .background(selectedCategory == cat ? Color.accentColor : Color(UIColor.secondarySystemFill))
                                .foregroundColor(selectedCategory == cat ? .white : .primary)
                                .cornerRadius(20)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }

            if filteredDocuments.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "doc.viewfinder")
                        .font(.system(size: 64))
                        .foregroundColor(.accentColor)
                    Text("DocScanner Offline AI")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("Tap the scan button to scan your first document with AI edge detection")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
                Spacer()
            } else if isGridView {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 14) {
                        ForEach(filteredDocuments) { doc in
                            NavigationLink(destination: DocumentDetailView(document: doc)) {
                                DocumentGridCard(document: doc)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    .padding(16)
                }
            } else {
                List {
                    ForEach(filteredDocuments) { doc in
                        NavigationLink(destination: DocumentDetailView(document: doc)) {
                            HStack(spacing: 12) {
                                Text(doc.categoryEmoji)
                                    .font(.title2)
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text(doc.title).font(.headline).lineLimit(1)
                                        if doc.isPinned {
                                            Text("📌").font(.caption)
                                        }
                                    }
                                    Text("\(doc.category) • \(doc.pageCount) pages")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .onDelete { indexSet in
                        for index in indexSet {
                            store.deleteDocument(filteredDocuments[index])
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }
        }
        .searchable(text: $searchQuery, prompt: "Search documents & OCR text...")
        .navigationTitle("DocScanner")
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { isShowingSettings = true }) {
                    Image(systemName: "gearshape")
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                HStack(spacing: 12) {
                    Button(action: { isGridView.toggle() }) {
                        Image(systemName: isGridView ? "list.bullet" : "square.grid.2x2")
                    }
                    Button(action: { isShowingScanner = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                            .foregroundColor(.accentColor)
                    }
                }
            }
        }
        .sheet(isPresented: $isShowingScanner) {
            ScannerView { images in
                if !images.isEmpty {
                    store.addDocument(
                        title: "Scan \(DateFormatter.localizedString(from: Date(), dateStyle: .short, timeStyle: .short))",
                        category: "Notes",
                        pageImages: images,
                        extractedText: ""
                    )
                }
            }
        }
        .sheet(isPresented: $isShowingSettings) {
            NavigationView {
                SettingsView()
            }
        }
    }
}

struct DocumentGridCard: View {
    let document: DocumentItem

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack(alignment: .topTrailing) {
                if let uiImage = UIImage(contentsOfFile: document.thumbnailPath) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                        .frame(height: 140)
                        .clipped()
                        .cornerRadius(10)
                } else {
                    Rectangle()
                        .fill(Color(UIColor.secondarySystemFill))
                        .frame(height: 140)
                        .cornerRadius(10)
                        .overlay(
                            Text(document.categoryEmoji)
                                .font(.system(size: 40))
                        )
                }

                if document.isPinned {
                    Text("📌")
                        .font(.caption)
                        .padding(5)
                        .background(Color.white.opacity(0.85))
                        .clipShape(Circle())
                        .padding(6)
                }
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(document.title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(1)
                    .foregroundColor(.primary)

                HStack {
                    Text(document.categoryEmoji + " " + document.category)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("\(document.pageCount)p")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.accentColor.opacity(0.15))
                        .foregroundColor(.accentColor)
                        .cornerRadius(6)
                }
            }
            .padding(.horizontal, 4)
            .padding(.bottom, 6)
        }
        .padding(8)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(14)
    }
}

