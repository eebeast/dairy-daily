# slip-ocr Delta

## MODIFIED Requirements

### Requirement: Extract fields from slip image
The system SHALL perform on-device text recognition on the captured slip image and extract quantity, FAT, SNF, rate, total, and optional morning/evening shift using keyword-based matching against common dairy slip labels. Missing or ambiguous shift text SHALL leave shift blank, without inferring it from time, entry order, or existing sales.

#### Scenario: All fields extracted from clean slip
- **WHEN** user captures a clear photo of a printed slip containing "Qty: 5.2", "FAT: 6.5", "SNF: 8.8", "Rate: 38.50", "Total: 200.20", and "Shift: Morning"
- **THEN** system extracts quantity 5.2, FAT 6.5, SNF 8.8, rate 38.50, total 200.20, and morning shift

#### Scenario: Partial extraction from unclear slip
- **WHEN** OCR recognizes quantity and rate but not FAT or SNF
- **THEN** system fills recognized fields and leaves unrecognized fields empty for manual input

#### Scenario: No text recognized
- **WHEN** image is too blurry or dark for any text recognition
- **THEN** system informs the user that no data could be extracted and offers manual entry or retake

#### Scenario: Evening shift recognized
- **WHEN** recognized slip text contains the unambiguous label "Shift: Evening"
- **THEN** evening is pre-filled for review

#### Scenario: Shift absent or conflicting
- **WHEN** recognized slip text contains valid sale fields but no shift label, an unsupported shift, or conflicting morning and evening labels
- **THEN** sale fields are pre-filled and shift remains blank

### Requirement: Present extracted data for confirmation
The system SHALL display all extracted values in an editable form before saving, allowing the user to correct any misread values and select or clear an optional shift. Missing shift SHALL not block saving otherwise valid sale data. Returning to the same scan SHALL preserve the user's corrections.

#### Scenario: User confirms correct extraction
- **WHEN** all extracted values match the slip and user taps confirm
- **THEN** entry is saved with the displayed values

#### Scenario: User corrects a misread value
- **WHEN** OCR extracts FAT as 6.8 but slip shows 6.5
- **THEN** user edits FAT to 6.5, taps confirm, and entry is saved with corrected value

#### Scenario: Save with no detected shift
- **WHEN** valid sale fields are extracted and shift is blank and user confirms without selecting a shift
- **THEN** the entry is saved without a shift

#### Scenario: Correct or clear recognized shift
- **WHEN** OCR recognizes morning and user changes it to evening or clears it before confirming
- **THEN** the entry is saved with the user's selected shift or with no shift, subject to known-shift duplicate validation

#### Scenario: Revisit same scan after correction
- **WHEN** user clears an extracted shift and the same scan's confirmation recomposes or resumes
- **THEN** shift remains blank and is not re-applied from the original scan

#### Scenario: Retake has no recognizable shift
- **WHEN** an earlier scan recognized morning but a new scan has valid numeric fields without recognizable shift text
- **THEN** the new scan's review leaves shift blank without retaining morning
