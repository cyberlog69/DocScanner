# Interactive Screenshot Session for Google Play & F-Droid Fastlane
$steps = @(
    @{ Name = "1_home"; Description = "Home Dashboard (Document list, folder bar, category chips, search)" },
    @{ Name = "2_document_detail"; Description = "Document Detail (Multi-page viewer, OCR text, Receipt financial card)" },
    @{ Name = "3_id_card_mode"; Description = "ID Card Mode (2-step front/back capture or composited A4 view)" },
    @{ Name = "4_encrypted_vault"; Description = "Encrypted Vault (Biometric unlock prompt or unlocked vault list)" },
    @{ Name = "5_watermark_stamps"; Description = "PDF Watermark Stamps (Stamp picker dialog with APPROVED/CONFIDENTIAL)" },
    @{ Name = "6_offline_backup"; Description = "Settings & Backup (100% Offline Backup & Restore card)" }
)

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  DocScanner Screenshot Capture Session (via ADB)      " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

foreach ($step in $steps) {
    Write-Host "`nStep: $($step.Name)" -ForegroundColor Yellow
    Write-Host "Instruction: Open $($step.Description) on your phone." -ForegroundColor White
    Read-Host "Press [Enter] when the screen is ready on your phone (or type 's' to skip)" | Out-Null
    
    powershell -ExecutionPolicy Bypass -File "scripts\take_screenshot.ps1" -Name $step.Name
}

Write-Host "`nAll screenshots captured successfully!" -ForegroundColor Green
Write-Host "Check your screenshots in:" -ForegroundColor White
Write-Host " - DocScanner\fastlane\metadata\android\en-US\images\phoneScreenshots" -ForegroundColor Yellow
Write-Host " - builds\screenshots" -ForegroundColor Yellow
