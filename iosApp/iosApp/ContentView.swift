import SwiftUI

struct ContentView: View {
    @StateObject private var store = DocumentStore()

    var body: some View {
        NavigationView {
            DocumentListView()
        }
        .environmentObject(store)
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

