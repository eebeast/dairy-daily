# slip-ocr Specification

## Purpose

Extracts milk sale data from printed dairy slips via camera-based OCR, reducing manual data entry to a quick confirmation step.

## Requirements

### Requirement: Capture slip image via camera
The system SHALL open the device camera to capture a photo of a printed dairy slip when the user initiates OCR scan.

#### Scenario: Camera opens for capture
- **WHEN** user taps the scan slip button
- **THEN** device camera opens in capture mode

#### Scenario: User cancels camera
- **WHEN** user dismisses the camera without taking a photo
- **THEN** system returns to the previous screen without creating an entry

### Requirement: Extract fields from slip image
The system SHALL perform on-device text recognition on the captured slip image and extract quantity, FAT, SNF, rate, and total using keyword-based matching against common dairy slip labels.

#### Scenario: All fields extracted from clean slip
- **WHEN** user captures a clear photo of a printed slip containing "Qty: 5.2", "FAT: 6.5", "SNF: 8.8", "Rate: 38.50", "Total: 200.20"
- **THEN** system extracts quantity 5.2, FAT 6.5, SNF 8.8, rate 38.50, and total 200.20

#### Scenario: Partial extraction from unclear slip
- **WHEN** OCR recognizes quantity and rate but not FAT or SNF
- **THEN** system fills recognized fields and leaves unrecognized fields empty for manual input

#### Scenario: No text recognized
- **WHEN** image is too blurry or dark for any text recognition
- **THEN** system informs the user that no data could be extracted and offers manual entry or retake

### Requirement: Present extracted data for confirmation
The system SHALL display all extracted values in an editable form before saving, allowing the user to correct any misread values.

#### Scenario: User confirms correct extraction
- **WHEN** all extracted values match the slip and user taps confirm
- **THEN** entry is saved with the displayed values

#### Scenario: User corrects a misread value
- **WHEN** OCR extracts FAT as 6.8 but slip shows 6.5
- **THEN** user edits FAT to 6.5, taps confirm, and entry is saved with corrected value

### Requirement: Cross-verify OCR-extracted total
The system SHALL compute `quantity × rate` from extracted values and compare against the extracted total. A mismatch exceeding ₹0.50 SHALL be highlighted to the user on the confirmation screen.

#### Scenario: Mismatch detected on confirmation screen
- **WHEN** extracted quantity is 5.2, rate is 38.50 (calculated 200.20) and extracted total is 280.20
- **THEN** system highlights the discrepancy before save

### Requirement: OCR works without network
On-device text recognition SHALL function without any network connectivity.

#### Scenario: OCR in airplane mode
- **WHEN** device is in airplane mode and user captures a slip photo
- **THEN** text recognition runs locally and extracts fields normally

### Requirement: Camera permission handling
The system SHALL request camera permission before first use and SHALL provide a clear explanation of why camera access is needed. If permission is denied, the system SHALL guide the user to enable it in settings or offer manual entry.

#### Scenario: Permission granted
- **WHEN** user grants camera permission on first scan attempt
- **THEN** camera opens normally

#### Scenario: Permission denied
- **WHEN** user denies camera permission
- **THEN** system shows explanation and offers to open app settings or switch to manual entry
