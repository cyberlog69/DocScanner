import SwiftUI
// import shared  // Uncomment after building the KMP shared.xcframework on Mac

/// Document list screen — mirrors Android DocumentListScreen.kt
struct DocumentListView: View {
    // TODO: Replace with shared KMP DocumentListViewModel via Kotlin/Native interop
    @State private var documents: [MockDocument] = []
    @State private var isShowingScanner = false

    var body: some View {
        List(documents, id: \.id) { doc in
            NavigationLink(destination: DocumentDetailView(document: doc)) {
                VStack(alignment: .leading) {
                    Text(doc.title).font(.headline)
                    Text("\(doc.category) • \(doc.pageCount) pages").font(.caption).foregroundColor(.secondary)
                }
            }
        }
        .navigationTitle("DocScanner")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { isShowingScanner = true }) {
                    Image(systemName: "doc.viewfinder")
                }
            }
        }
        .sheet(isPresented: $isShowingScanner) {
            ScannerView()
        }
    }
}

// MARK: — Placeholder until KMP shared framework is linked
struct MockDocument {
    let id: String
    let title: String
    let category: String
    let pageCount: Int
}
