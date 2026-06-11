## ADDED Requirements

### Requirement: Icon buttons SHALL have contentDescription

All PixelIconButton instances SHALL have a `contentDescription` parameter for screen reader accessibility.

#### Scenario: Play button has accessibility label
- **WHEN** PixelIconButton is used for play action
- **THEN** it has contentDescription = "开始"

#### Scenario: Delete button has accessibility label
- **WHEN** PixelIconButton is used for delete action
- **THEN** it has contentDescription = "删除"

#### Scenario: Add button has accessibility label
- **WHEN** PixelIconButton is used for add action
- **THEN** it has contentDescription = "添加"

### Requirement: Buttons SHALL have Chinese text labels

All button text SHALL be in Chinese, no English text.

#### Scenario: Dialog confirm button uses Chinese
- **WHEN** a dialog displays a confirm button
- **THEN** the button text is "确定" (not "OK" or "Confirm")

#### Scenario: Dialog cancel button uses Chinese
- **WHEN** a dialog displays a cancel button
- **THEN** the button text is "取消" (not "Cancel")
