import Foundation
import UIKit
import Vision

/// Native on-device OCR engine for iOS using Apple's Vision framework (Neural Engine accelerated).
public class OcrService {
    public static let shared = OcrService()

    private init() {}

    /// Performs text recognition on a single UIImage.
    public func recognizeText(from image: UIImage, completion: @escaping (String) -> Void) {
        guard let cgImage = image.cgImage else {
            completion("")
            return
        }

        let request = VNRecognizeTextRequest { request, error in
            guard let observations = request.results as? [VNRecognizedTextObservation], error == nil else {
                completion("")
                return
            }

            let lines = observations.compactMap { observation in
                observation.topCandidates(1).first?.string
            }

            let fullText = lines.joined(separator: "\n")
            completion(fullText.trimmingCharacters(in: .whitespacesAndNewlines))
        }

        request.recognitionLevel = .accurate
        request.usesLanguageCorrection = true
        request.recognitionLanguages = [
            "en-US", "es-ES", "fr-FR", "de-DE", "it-IT", "pt-BR",
            "zh-Hans", "zh-Hant", "ja-JP", "ko-KR", "ru-RU", "uk-UA"
        ]

        let orientation = CGImagePropertyOrientation(image.imageOrientation)
        let handler = VNImageRequestHandler(cgImage: cgImage, orientation: orientation, options: [:])

        DispatchQueue.global(qos: .userInitiated).async {
            do {
                try handler.perform([request])
            } catch {
                DispatchQueue.main.async {
                    completion("")
                }
            }
        }
    }

    /// Performs text recognition across an array of UIImages (e.g. multi-page scan).
    public func recognizeText(from images: [UIImage], completion: @escaping ([String], String) -> Void) {
        guard !images.isEmpty else {
            completion([], "")
            return
        }

        let group = DispatchGroup()
        var pageResults = [Int: String]()
        let lock = NSLock()

        for (index, image) in images.enumerated() {
            group.enter()
            recognizeText(from: image) { recognizedText in
                lock.lock()
                pageResults[index] = recognizedText
                lock.unlock()
                group.leave()
            }
        }

        group.notify(queue: .main) {
            var pages = [String]()
            for i in 0..<images.count {
                let pageText = pageResults[i] ?? ""
                pages.append(pageText)
            }
            let combined = pages.filter { !$0.isEmpty }.joined(separator: "\n\n--- Page Break ---\n\n")
            completion(pages, combined)
        }
    }

    /// Intelligently classifies document category based on extracted OCR text keywords.
    public func classifyCategory(from text: String) -> String {
        let lower = text.lowercased()

        // 1. Receipts
        let receiptKeywords = [
            "total", "subtotal", "tax", "cash", "credit card", "debit",
            "change due", "receipt", "items sold", "qty", "merchant", "store #", "thank you for shopping"
        ]
        if receiptKeywords.filter({ lower.contains($0) }).count >= 2 {
            return "Receipts"
        }

        // 2. Invoices
        let invoiceKeywords = [
            "invoice", "bill to", "invoice date", "due date", "amount due",
            "po number", "invoice #", "remit to", "balance due", "payment terms", "vat reg"
        ]
        if invoiceKeywords.filter({ lower.contains($0) }).count >= 2 {
            return "Invoices"
        }

        // 3. ID Cards / Government Documents
        let idKeywords = [
            "driver license", "driving licence", "identity card", "passport", "date of birth",
            "dob:", "sex:", "expiry date", "issuing authority", "national id", "republic", "identification", "citizen"
        ]
        if idKeywords.contains(where: { lower.contains($0) }) {
            return "ID Cards"
        }

        // 4. Contracts / Legal Agreements
        let contractKeywords = [
            "agreement", "contract", "parties", "hereby agrees", "terms and conditions",
            "signed by", "in witness whereof", "confidentiality", "indemnification", "jurisdiction", "governing law"
        ]
        if contractKeywords.filter({ lower.contains($0) }).count >= 2 {
            return "Contracts"
        }

        // 5. Books / Academic
        let bookKeywords = [
            "chapter", "table of contents", "preface", "isbn", "copyright ©",
            "publisher", "edition", "bibliography", "prologue", "epilogue"
        ]
        if bookKeywords.contains(where: { lower.contains($0) }) {
            return "Books"
        }

        return "Notes"
    }

    /// Suggests a smart document title from the extracted OCR text (first meaningful heading).
    public func extractSmartTitle(from text: String, fallback: String) -> String {
        let lines = text.components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty && $0.count >= 3 && $0.count <= 50 }

        if let firstHeader = lines.first {
            // Clean up any leading/trailing symbols
            let clean = firstHeader.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
            if clean.count >= 3 {
                return clean
            }
        }
        return fallback
    }
}

// Helper to convert UIImageOrientation to CGImagePropertyOrientation
private extension CGImagePropertyOrientation {
    init(_ uiOrientation: UIImage.Orientation) {
        switch uiOrientation {
        case .up: self = .up
        case .upMirrored: self = .upMirrored
        case .down: self = .down
        case .downMirrored: self = .downMirrored
        case .left: self = .left
        case .leftMirrored: self = .leftMirrored
        case .right: self = .right
        case .rightMirrored: self = .rightMirrored
        @unknown default: self = .up
        }
    }
}
