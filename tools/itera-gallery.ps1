[CmdletBinding()]
param(
    [ValidateSet('ScreenColumn','IteraCard','Divider','Eyebrow','Pill','IteraButton','CircleIconButton','TopBar','TechniqueToken','ChoiceChip','Segmented','StepRow','StepDot','MasteryLadder','ProgressBar','NoteField','SectionTitle','CheckCircle','RadioDot','LinkRow','MasteryDots','IntervalLadder','AnimatedCheck','Group','ValueRow','SwitchRow','TimeRow','LanguageSheet','LanguagePill','TimePickerSheet','EmptyState','ErrorState','Skeleton','Shell','Tokens')]
    [string[]]$Pages = @('Shell'),
    [ValidateSet('false','true')][string]$Dark = 'false',
    [ValidateSet(1,2)][int]$FontScale = 1,
    [switch]$All
)
$ErrorActionPreference = 'Stop'
$adb = 'G:\Android\SDK\platform-tools\adb.exe'
$root = 'D:\Projects\Itera\captures'
if ($All) { $Pages = @('ScreenColumn','IteraCard','Divider','Eyebrow','Pill','IteraButton','CircleIconButton','TopBar','TechniqueToken','ChoiceChip','Segmented','StepRow','StepDot','MasteryLadder','ProgressBar','NoteField','SectionTitle','CheckCircle','RadioDot','LinkRow','MasteryDots','IntervalLadder','AnimatedCheck','Group','ValueRow','SwitchRow','TimeRow','LanguageSheet','LanguagePill','TimePickerSheet','EmptyState','ErrorState','Skeleton','Shell','Tokens') }
foreach ($page in $Pages) {
    foreach ($app in @('com.itera.app','com.wivernz.itera')) {
        & $adb -P 5038 -s 127.0.0.1:5555 shell am start -S -W -n "$app/.GalleryActivity" --es page $page --ez dark $Dark --ef fontScale "$FontScale.0" | Out-Null
        if ($LASTEXITCODE) { throw 'Gallery launch failed' }
        Start-Sleep -Milliseconds 1500
        & $adb -P 5038 -s 127.0.0.1:5555 shell screencap -p /sdcard/itera-gallery.png
        if ($LASTEXITCODE) { throw 'Capture failed' }
        $name = "$app-$page-$Dark-$FontScale.png"
        & $adb -P 5038 -s 127.0.0.1:5555 pull /sdcard/itera-gallery.png (Join-Path $root $name) | Out-Null
        if ($LASTEXITCODE) { throw 'Pull failed' }
        Write-Output $name
        & $adb -P 5038 -s 127.0.0.1:5555 shell input keyevent KEYCODE_BACK
    }
}
