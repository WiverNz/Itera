[CmdletBinding()]
param(
    [ValidateSet('ScreenColumn','IteraCard','Divider','Eyebrow','Pill','IteraButton','CircleIconButton','TopBar','TechniqueToken','ChoiceChip','Segmented','StepRow','StepDot','MasteryLadder','ProgressBar','NoteField','SectionTitle','CheckCircle','RadioDot','LinkRow','MasteryDots','IntervalLadder','AnimatedCheck','Group','ValueRow','SwitchRow','TimeRow','LanguageSheet','LanguagePill','TimePickerSheet','EmptyState','ErrorState','Skeleton','Shell','Tokens')]
    [string[]]$Pages = @('Shell'),
    [ValidateSet('false','true')][string]$Dark = 'false',
    [ValidateSet(1,2)][int]$FontScale = 1,
    [switch]$All, [switch]$Final, [ValidateSet(5555,5561)][int]$DevicePort = 5555
)
$ErrorActionPreference = 'Stop'
$adb = 'G:\Android\SDK\platform-tools\adb.exe'
$root = 'D:\Projects\Itera\captures'
if ($All) { $Pages = @('ScreenColumn','IteraCard','Divider','Eyebrow','Pill','IteraButton','CircleIconButton','TopBar','TechniqueToken','ChoiceChip','Segmented','StepRow','StepDot','MasteryLadder','ProgressBar','NoteField','SectionTitle','CheckCircle','RadioDot','LinkRow','MasteryDots','IntervalLadder','AnimatedCheck','Group','ValueRow','SwitchRow','TimeRow','LanguageSheet','LanguagePill','TimePickerSheet','EmptyState','ErrorState','Skeleton','Shell','Tokens') }
if ($Final) { $Pages = @('Group','ValueRow','LanguageSheet','TimeRow','IntervalLadder','TimePickerSheet') }
foreach ($page in $Pages) {
    foreach ($app in @('com.itera.app','com.wivernz.itera')) {
        $launch = & $adb -P 5038 -s "127.0.0.1:$DevicePort" shell am start -W -f 0x10008000 -n "$app/.GalleryActivity" --es page $page --ez dark $Dark --ef fontScale "$FontScale.0"
        if ($LASTEXITCODE -or -not ($launch -match 'Status: ok')) { throw 'Gallery did not finish launching; no screenshot captured' }
        Start-Sleep -Milliseconds 1500
        & $adb -P 5038 -s "127.0.0.1:$DevicePort" shell screencap -p /sdcard/itera-gallery.png
        if ($LASTEXITCODE) { throw 'Capture failed' }
        $name = "$app-$page-$Dark-$FontScale.png"
        & $adb -P 5038 -s "127.0.0.1:$DevicePort" pull /sdcard/itera-gallery.png (Join-Path $root $name) | Out-Null
        if ($LASTEXITCODE) { throw 'Pull failed' }
        Write-Output $name
        & $adb -P 5038 -s "127.0.0.1:$DevicePort" shell input keyevent KEYCODE_BACK
    }
}
